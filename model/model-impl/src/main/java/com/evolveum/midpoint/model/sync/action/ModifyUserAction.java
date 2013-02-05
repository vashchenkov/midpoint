/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.CompiletimeConfig;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class ModifyUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyUserAction.class);
    /**
     * Action name for operation result
     */
    private final String actionName;
    /**
     * Decision regarding the user. If set to null user activation won't be changed. If set to
     * {@link ActivationDecision#DISABLE} ({@link ActivationDecision#ENABLE}) user will be disabled (enabled),
     */
    private ActivationDecision userActivationDecision;
    /**
     * Decision regarding the account. If set to null account activation won't be changed. If set to
     * {@link ActivationDecision#DISABLE} ({@link ActivationDecision#ENABLE}) account will be disabled (enabled),
     */
    private ActivationDecision accountActivationDecision;
    /**
     * Decision regarding account state see {@link SynchronizationPolicyDecision}.
     */
    private SynchronizationIntent accountSynchronizationIntent;

    public ModifyUserAction() {
        this(SynchronizationIntent.KEEP, ACTION_MODIFY_USER);
    }

    public ModifyUserAction(SynchronizationIntent accountSyncIntent, String actionName) {
        Validate.notEmpty(actionName, "Action name must not be null or empty.");
        this.accountSynchronizationIntent = accountSyncIntent;
        this.actionName = actionName;
    }

    protected void setAccountActivationDecision(ActivationDecision decision) {
        this.accountActivationDecision = decision;
    }

    protected void setUserActivationDecision(ActivationDecision decision) {
        this.userActivationDecision = decision;
    }

    protected SynchronizationIntent getAccountSynchronizationIntent() {
        return accountSynchronizationIntent;
    }

    protected ActivationDecision getUserActivationDecision() {
        return userActivationDecision;
    }

    protected ActivationDecision getAccountActivationDecision() {
        return accountActivationDecision;
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, 
            OperationResult result) throws SynchronizationException, SchemaException {
        super.executeChanges(userOid, change, situation, auditRecord, task, result);

        Class<? extends ResourceObjectShadowType> clazz = getClassFromChange(change);
        if (!AccountShadowType.class.isAssignableFrom(clazz)) {
            throw new SynchronizationException("Couldn't synchronize shadow of type '"
                    + clazz + "', only '" + AccountShadowType.class.getName() + "' is supported.");
        }

        OperationResult subResult = result.createSubresult(actionName);
        if (StringUtils.isEmpty(userOid)) {
            String message = "Can't modify user, user oid is empty or null.";
            subResult.recordFatalError(message);
            throw new SynchronizationException(message);
        }

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.recordFatalError(message);
            throw new SynchronizationException(message);
        }

        LensContext<UserType, AccountShadowType> context = null;
        LensProjectionContext<AccountShadowType> accountContext = null;
        try {
            context = createSyncContext(userType, change.getResource().asObjectable(), change);
            accountContext = createAccountLensContext(context, change,
                    getAccountSynchronizationIntent(), getAccountActivationDecision());
            if (accountContext == null) {
                LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                return userOid;
            }
        } catch (Exception ex) {
        	subResult.recordFatalError("Couldn't update account sync context in modify user action: "+ex.getMessage(), ex);
            throw new SynchronizationException("Couldn't update account sync context in modify user action.", ex);
        }
        
        updateContextBeforeSync(context, accountContext);

        try {
            synchronizeUser(context, task, subResult);
        } finally {
            subResult.recomputeStatus();
            result.recomputeStatus();
            
//            auditRecord.clearTimestamp();
//            auditRecord.setEventType(AuditEventType.MODIFY_OBJECT);
//        	auditRecord.setEventStage(AuditEventStage.EXECUTION);
//        	auditRecord.setResult(result);
//        	auditRecord.clearDeltas();
//        	auditRecord.addDeltas(context.getAllChanges());
//        	getAuditService().audit(auditRecord, task);
        }

        return userOid;
    }

    /**
	 * A chance to update the context before a sync is executed. For use in subclasses.
	 */
	protected void updateContextBeforeSync(LensContext<UserType, AccountShadowType> context, 
			LensProjectionContext<AccountShadowType> accountContext) {
		// Nothing to do here
	}

	private Class<? extends ResourceObjectShadowType> getClassFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getObjectDelta() != null) {
            return change.getObjectDelta().getObjectTypeClass();
        }

        if (change.getCurrentShadow() != null) {
            return change.getCurrentShadow().getCompileTimeClass();
        }

        return change.getOldShadow().getCompileTimeClass();
    }

    private LensContext<UserType, AccountShadowType> createSyncContext(UserType user, ResourceType resource, ResourceObjectShadowChangeDescription change) throws SchemaException {
        LOGGER.trace("Creating sync context.");

        PrismObjectDefinition<UserType> userDefinition = getPrismContext().getSchemaRegistry().findObjectDefinitionByType(
                SchemaConstants.I_USER_TYPE);

        LensContext<UserType, AccountShadowType> context = createEmptyLensContext(change);
        LensFocusContext<UserType> focusContext = context.createFocusContext();
        PrismObject<UserType> oldUser = user.asPrismObject();
        focusContext.setObjectOld(oldUser);
        context.rememberResource(resource);

        //check and update activation if necessary
        if (userActivationDecision == null) {
            LOGGER.trace("User activation decision not defined, skipping activation check.");
            return context;
        }

        PrismProperty enable = oldUser.findOrCreateProperty(SchemaConstants.PATH_ACTIVATION_ENABLE);
        LOGGER.trace("User activation defined, activation property found {}", enable);

        PrismPropertyValue<Boolean> value = enable.getValue(Boolean.class);
        if (value != null) {
            Boolean isEnabled = value.getValue();
            if (isEnabled == null) {
                createActivationPropertyDelta(context, userActivationDecision, null);
            }

            if ((isEnabled && ActivationDecision.DISABLE.equals(userActivationDecision))
                    || (!isEnabled && ActivationDecision.ENABLE.equals(userActivationDecision))) {

                createActivationPropertyDelta(context, userActivationDecision, isEnabled);
            }
        } else {
            createActivationPropertyDelta(context, userActivationDecision, null);
        }

        return context;
    }

    private void createActivationPropertyDelta(LensContext<UserType, AccountShadowType> context, ActivationDecision activationDecision,
            Boolean oldValue) {

    	LensFocusContext<UserType> focusContext = context.getFocusContext();
        ObjectDelta<UserType> userDelta = focusContext.getSecondaryDelta(0);
        if (userDelta == null) {
            userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, getPrismContext());
            userDelta.setOid(focusContext.getObjectOld().getOid());
            focusContext.setSecondaryDelta(userDelta, 0);
        }

        createActivationPropertyDelta(userDelta, activationDecision, oldValue);
    }
}
