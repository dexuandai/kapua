/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.console.client.device.ui.dialog;

import org.eclipse.kapua.app.console.client.messages.ConsoleMessages;
import org.eclipse.kapua.app.console.client.util.FailureHandler;
import org.eclipse.kapua.app.console.client.util.KapuaSafeHtmlUtils;
import org.eclipse.kapua.app.console.shared.model.GwtDevice;
import org.eclipse.kapua.app.console.shared.model.GwtDeviceQueryPredicates;
import org.eclipse.kapua.app.console.shared.model.GwtGroup;
import org.eclipse.kapua.app.console.shared.model.GwtSession;
import org.eclipse.kapua.app.console.shared.model.GwtXSRFToken;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class DeviceEditDialog extends DeviceAddDialog {

    private GwtDevice selectedDevice;

    private static final ConsoleMessages MSGS = GWT.create(ConsoleMessages.class);

    public DeviceEditDialog(GwtSession currentSession, GwtDevice selectedDevice) {
        super(currentSession);
        this.selectedDevice = selectedDevice;
    }

    @Override
    public void createBody() {
        generateBody();

        // hide fields used for add
        clientIdField.hide();

        loadDevice();
    }

    private void loadDevice() {
        maskDialog();
        gwtDeviceService.findDevice(selectedDevice.getScopeId(), selectedDevice.getClientId(), new AsyncCallback<GwtDevice>() {

            @Override
            public void onSuccess(GwtDevice gwtDevice) {
                unmaskDialog();
                populateDialog(gwtDevice);
            }

            @Override
            public void onFailure(Throwable cause) {
                exitStatus = false;
                // FIXME
                exitMessage = MSGS.error();
                // exitMessage = MSGS.dialogEditLoadFailed(cause.getLocalizedMessage());
                unmaskDialog();
                hide();
            }
        });

    }

    @Override
    public void submit() {
        // General info
        selectedDevice.setDisplayName(KapuaSafeHtmlUtils.htmlUnescape(displayNameField.getValue()));
        selectedDevice.setGwtDeviceStatus(statusCombo.getSimpleValue().name());
        selectedDevice.setGroupId(groupCombo.getValue().getId());

        // Security Options
        // m_selectedDevice.setCredentialsTight(GwtDeviceCredentialsTight.getEnumFromLabel(credentialsTightCombo.getSimpleValue()).name());
        // m_selectedDevice.setCredentialsAllowChange(allowCredentialsChangeCheckbox.getValue());
        // m_selectedDevice.setDeviceUserId(deviceUserCombo.getValue().getId());

        // Custom attributes
        selectedDevice.setCustomAttribute1(KapuaSafeHtmlUtils.htmlUnescape(customAttribute1Field.getValue()));
        selectedDevice.setCustomAttribute2(KapuaSafeHtmlUtils.htmlUnescape(customAttribute2Field.getValue()));
        selectedDevice.setCustomAttribute3(KapuaSafeHtmlUtils.htmlUnescape(customAttribute3Field.getValue()));
        selectedDevice.setCustomAttribute4(KapuaSafeHtmlUtils.htmlUnescape(customAttribute4Field.getValue()));
        selectedDevice.setCustomAttribute5(KapuaSafeHtmlUtils.htmlUnescape(customAttribute5Field.getValue()));

        // Optlock
        selectedDevice.setOptlock(optlock.getValue().intValue());

        //
        // Getting XSRF token
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtDeviceService.updateAttributes(token, selectedDevice, new AsyncCallback<GwtDevice>() {

                    public void onFailure(Throwable caught) {
                        // FailureHandler.handle(caught);
                        exitStatus = false;
                        // FIXME:
                        exitMessage = MSGS.error();
                    }

                    public void onSuccess(GwtDevice gwtDevice) {
                        //
                        // Getting XSRF token
                        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                // FailureHandler.handle(ex);
                                exitStatus = false;
                                // FIXME:
                                exitMessage = MSGS.error();
                            }

                            @Override
                            public void onSuccess(GwtXSRFToken token) {
                                exitStatus = true;
                                exitMessage = MSGS.deviceUpdateSuccess();
                                hide();
                            }
                        });

                    }
                });
            }
        });
    }

    @Override
    public String getHeaderMessage() {
        return MSGS.deviceFormHeadingEdit(this.selectedDevice.getDisplayName() != null ? this.selectedDevice.getDisplayName() : this.selectedDevice.getClientId());
    }

    @Override
    public String getInfoMessage() {
        // FIXME
        return "";
        // return MSGS.dialogEditInfo();
    }

    private void populateDialog(GwtDevice device) {
        if (device != null) {
            // General info data
            clientIdLabel.setText(device.getClientId());
            displayNameField.setValue(device.getUnescapedDisplayName());
            statusCombo.setSimpleValue(GwtDeviceQueryPredicates.GwtDeviceStatus.valueOf(device.getGwtDeviceStatus()));

            // Security options data
            // credentialsTightCombo.setSimpleValue(m_selectedDevice.getCredentialTightEnum().getLabel());
            // allowCredentialsChangeCheckbox.setValue(m_selectedDevice.getCredentialsAllowChange());
            // gwtUserService.find(m_currentSession.getSelectedAccount().getId(), m_selectedDevice.getDeviceUserId(), new AsyncCallback<GwtUser>() {
            // @Override
            // public void onFailure(Throwable caught)
            // {
            // FailureHandler.handle(caught);
            // }
            //
            // @Override
            // public void onSuccess(GwtUser gwtUser)
            // {
            // deviceUserCombo.setValue(gwtUser);
            // }
            // });
            if (device.getGroupId() != null) {
                gwtGroupService.find(currentSession.getSelectedAccount().getId(), device.getGroupId(), new AsyncCallback<GwtGroup>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(GwtGroup result) {
                        groupCombo.setValue(result);
                    }
                });
            } else {
                groupCombo.setValue(NO_GROUP);
            }
            // // Custom attributes data
            customAttribute1Field.setValue(device.getUnescapedCustomAttribute1());
            customAttribute2Field.setValue(device.getUnescapedCustomAttribute2());
            customAttribute4Field.setValue(device.getUnescapedCustomAttribute4());
            customAttribute3Field.setValue(device.getUnescapedCustomAttribute3());
            customAttribute5Field.setValue(device.getUnescapedCustomAttribute5());

            // Other data
            optlock.setValue(device.getOptlock());
        }
    }
}
