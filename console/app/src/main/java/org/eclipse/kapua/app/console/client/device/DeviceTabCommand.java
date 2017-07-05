/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.console.client.device;

import org.eclipse.kapua.app.console.client.messages.ConsoleMessages;
import org.eclipse.kapua.app.console.commons.client.util.Constants;
import org.eclipse.kapua.app.console.commons.client.util.FailureHandler;
import org.eclipse.kapua.app.console.commons.client.util.KapuaSafeHtmlUtils;
import org.eclipse.kapua.app.console.shared.model.GwtDevice;
import org.eclipse.kapua.app.console.commons.shared.model.GwtSession;
import org.eclipse.kapua.app.console.commons.shared.model.GwtXSRFToken;
import org.eclipse.kapua.app.console.commons.shared.service.GwtSecurityTokenService;
import org.eclipse.kapua.app.console.commons.shared.service.GwtSecurityTokenServiceAsync;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FormEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FileUploadField;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Encoding;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Method;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class DeviceTabCommand extends LayoutContainer {

    private static final ConsoleMessages MSGS = GWT.create(ConsoleMessages.class);

    private final static String SERVLET_URL = "file/command";

    private final static int COMMAND_TIMEOUT_SECS = 60;

    // XSRF
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private HiddenField<String> xsrfTokenField;

    private boolean dirty;
    private boolean initialized;
    private GwtDevice selectedDevice;

    private LayoutContainer commandInput;
    private FormPanel formPanel;
    private HiddenField<String> accountField;
    private HiddenField<String> deviceIdField;
    private HiddenField<Integer> timeoutField;

    private FileUploadField fileUploadField;
    private TextField<String> commandField;
    private TextField<String> passwordField;

    private ButtonBar buttonBar;
    private Button executeButton;
    private Button resetButton;

    private LayoutContainer commandOutput;
    private TextArea result;

    protected boolean resetProcess;

    public DeviceTabCommand(GwtSession currentSession) {
        dirty = false;
        initialized = false;
    }

    public void setDevice(GwtDevice selectedDevice) {
        dirty = true;
        this.selectedDevice = selectedDevice;
    }

    protected void onRender(Element parent, int index) {
        super.onRender(parent, index);

        setLayout(new FitLayout());
        setBorders(false);

        // init components
        initCommandInput();
        initCommandOutput();

        ContentPanel devicesCommandPanel = new ContentPanel();
        devicesCommandPanel.setBorders(false);
        devicesCommandPanel.setBodyBorder(false);
        devicesCommandPanel.setHeaderVisible(false);
        devicesCommandPanel.setScrollMode(Scroll.AUTO);
        devicesCommandPanel.setLayout(new FitLayout());

        devicesCommandPanel.setTopComponent(commandInput);
        devicesCommandPanel.add(commandOutput);

        add(devicesCommandPanel);
        initialized = true;
    }

    private void initCommandInput() {
        FormData formData = new FormData("-20");

        FormLayout layout = new FormLayout();
        layout.setLabelWidth(Constants.LABEL_WIDTH_FORM);

        FieldSet fieldSet = new FieldSet();
        fieldSet.setBorders(false);
        fieldSet.setLayout(layout);
        fieldSet.setStyleAttribute("margin", "0px");
        fieldSet.setStyleAttribute("padding", "0px");

        formPanel = new FormPanel();
        formPanel.setFrame(true);
        formPanel.setHeaderVisible(false);
        formPanel.setBorders(false);
        formPanel.setBodyBorder(false);
        formPanel.setAction(SERVLET_URL);
        formPanel.setEncoding(Encoding.MULTIPART);
        formPanel.setMethod(Method.POST);
        formPanel.add(fieldSet);
        formPanel.addListener(Events.Render, new Listener<BaseEvent>() {

            public void handleEvent(BaseEvent be) {
                NodeList<com.google.gwt.dom.client.Element> nl = formPanel.getElement().getElementsByTagName("form");
                if (nl.getLength() > 0) {
                    com.google.gwt.dom.client.Element elemForm = nl.getItem(0);
                    elemForm.setAttribute("autocomplete", "off");
                }
            }
        });

        formPanel.setButtonAlign(HorizontalAlignment.RIGHT);
        buttonBar = formPanel.getButtonBar();
        initButtonBar();

        formPanel.addListener(Events.BeforeSubmit, new Listener<BaseEvent>() {

            @Override
            public void handleEvent(BaseEvent be) {
                if (!selectedDevice.isOnline()) {
                    MessageBox.alert(MSGS.dialogAlerts(), MSGS.deviceOffline(), new Listener<MessageBoxEvent>() {

                        @Override
                        public void handleEvent(MessageBoxEvent be) {
                            commandInput.unmask();
                        }
                    });
                    be.setCancelled(true);
                }
            }
        });

        formPanel.addListener(Events.Submit, new Listener<FormEvent>() {

            @Override
            public void handleEvent(FormEvent be) {
                String htmlResult = be.getResultHtml();

                //
                // Some browsers will return <pre> (i.e. Firefox)
                // Other add styles and return <pre {some style}> (i.e. Safari, Chrome)
                //
                if (!htmlResult.startsWith("<pre")) {
                    int errorMessageStartIndex = htmlResult.indexOf("<pre");
                    errorMessageStartIndex = htmlResult.indexOf(">", errorMessageStartIndex) + 1;
                    int errorMessageEndIndex = htmlResult.indexOf("</pre>");

                    String errorMessage = htmlResult.substring(errorMessageStartIndex, errorMessageEndIndex);

                    MessageBox.alert(MSGS.error(), MSGS.fileUploadFailure() + ":<br/>" + errorMessage, null);
                    commandInput.unmask();
                } else {
                    int outputMessageStartIndex = htmlResult.indexOf("<pre");
                    outputMessageStartIndex = htmlResult.indexOf(">", outputMessageStartIndex) + 1;
                    int outputMessageEndIndex = htmlResult.indexOf("</pre>");

                    String output = htmlResult.substring(outputMessageStartIndex, outputMessageEndIndex);

                    result.setValue(KapuaSafeHtmlUtils.htmlUnescape(output));
                    commandInput.unmask();
                }
            }
        });

        accountField = new HiddenField<String>();
        accountField.setName("scopeIdString");
        fieldSet.add(accountField);

        deviceIdField = new HiddenField<String>();
        deviceIdField.setName("deviceIdString");
        fieldSet.add(deviceIdField);

        timeoutField = new HiddenField<Integer>();
        timeoutField.setName("timeout");
        fieldSet.add(timeoutField);

        //
        // xsrfToken Hidden field
        //
        xsrfTokenField = new HiddenField<String>();
        xsrfTokenField.setId("xsrfToken");
        xsrfTokenField.setName("xsrfToken");
        xsrfTokenField.setValue("");
        fieldSet.add(xsrfTokenField);

        commandField = new TextField<String>();
        commandField.setName("command");
        commandField.setAllowBlank(false);
        commandField.setFieldLabel("* " + MSGS.deviceCommandExecute());
        commandField.setLayoutData(layout);
        fieldSet.add(commandField, formData);

        fileUploadField = new FileUploadField();
        fileUploadField.setAllowBlank(true);
        fileUploadField.setName("file");
        fileUploadField.setLayoutData(layout);
        fileUploadField.setFieldLabel(MSGS.deviceCommandFile());
        fieldSet.add(fileUploadField, formData);

        passwordField = new TextField<String>();
        passwordField.setName("password");
        passwordField.setFieldLabel(MSGS.deviceCommandPassword());
        passwordField.setToolTip(MSGS.deviceCommandPasswordTooltip());
        passwordField.setPassword(true);
        passwordField.setLayoutData(layout);
        fieldSet.add(passwordField, formData);

        commandInput = formPanel;

        commandInput.mask(MSGS.deviceNoDeviceSelected());
    }

    private void initButtonBar() {
        executeButton = new Button(MSGS.deviceCommandExecute());
        executeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                if (formPanel.isValid()) {
                    result.clear();
                    commandInput.mask(MSGS.deviceCommandExecuting());
                    accountField.setValue(selectedDevice.getScopeId());
                    deviceIdField.setValue(selectedDevice.getId());
                    timeoutField.setValue(COMMAND_TIMEOUT_SECS);

                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex);
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            xsrfTokenField.setValue(token.getToken());
                            formPanel.submit();
                        }
                    });
                }
            }
        });

        resetButton = new Button(MSGS.reset());
        resetButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                if (!resetProcess) {
                    resetProcess = true;
                    resetButton.setEnabled(false);

                    formPanel.reset();

                    resetButton.setEnabled(true);
                    resetProcess = false;
                }
            }
        });

        buttonBar.add(resetButton);
        buttonBar.add(executeButton);
    }

    private void initCommandOutput() {
        commandOutput = new LayoutContainer();
        commandOutput.setBorders(false);
        commandOutput.setWidth("99.5%");
        commandOutput.setLayout(new FitLayout());

        result = new TextArea();
        result.setBorders(false);
        result.setReadOnly(true);
        result.setEmptyText(MSGS.deviceCommandNoOutput());
        result.setBorders(false);
        commandOutput.add(result);
    }

    // --------------------------------------------------------------------------------------
    //
    // Device Configuration Management
    //
    // --------------------------------------------------------------------------------------

    public void refresh() {
        if (dirty && initialized) {
            dirty = false;

            if (selectedDevice != null) {
                commandInput.unmask();
            }
        }
    }

    // --------------------------------------------------------------------------------------
    //
    // Unload of the GXT Component
    //
    // --------------------------------------------------------------------------------------

    public void onUnload() {
        super.onUnload();
    }

}
