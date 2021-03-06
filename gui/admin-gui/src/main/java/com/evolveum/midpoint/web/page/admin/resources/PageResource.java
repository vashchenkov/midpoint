/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceController;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceObjectTypeDto;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 * @author Michal Serbak
 */
@PageDescriptor(url = "/admin/resource", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL,
                label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL,
                description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
                label = "PageResource.auth.resource.label",
                description = "PageResource.auth.resource.description")})
public class PageResource extends PageAdminResources {

    private static final Trace LOGGER = TraceManager.getTrace(PageResource.class);

    private static final String DOT_CLASS = PageResource.class.getName() + ".";
    private static final String OPERATION_IMPORT_FROM_RESOURCE = DOT_CLASS + "importFromResource";
    private static final String TEST_CONNECTION = DOT_CLASS + "testConnection";

    private static final String ID_FORM_MAIN = "mainForm";
    private static final String ID_RESOURCE_OID = "resourceOid";
    private static final String ID_RESOURCE_NAME = "resourceName";
    private static final String ID_RESOURCE_TYPE = "resourceType";
    private static final String ID_RESOURCE_VERSION = "resourceVersion";
    private static final String ID_RESOURCE_PROGRESS = "resourceProgress";
    private static final String ID_CONNECTORS = "connectors";
    private static final String ID_OVERALL_STATUS = "overallStatus";
    private static final String ID_CONF_VALIDATION = "confValidation";
    private static final String ID_CON_INITIALIZATION = "conInitialization";
    private static final String ID_CON_CONNECTION = "conConnection";
    private static final String ID_CON_SCHEMA = "conSchema";
    private static final String ID_LIST_CAPABILITIES = "listCapabilities";
    private static final String ID_CAPABILITIES = "capabilities";
    private static final String ID_TABLE_OBJECT_TYPE = "objectTypesTable";
    private static final String ID_BUTTON_BACK = "back";
    private static final String ID_BUTTON_EDIT = "editResource";
    private static final String ID_BUTTON_TEST = "test";
    private static final String ID_BUTTON_IMPORT_ACCOUNTS = "importAccounts";
    private static final String ID_BUTTON_DELETE_SYNC_TOKEN = "deleteSyncToken";

    private IModel<ResourceDto> model;
    private PrismObject<ResourceType> resource;

    public PageResource() {
        initialize();
    }

    public PageResource(PageParameters parameters, PageTemplate previousPage) {
        getPageParameters().overwriteWith(parameters);
        setPreviousPage(previousPage);
        initialize();
    }

    private void initialize() {
        model = new LoadableModel<ResourceDto>() {

            @Override
            protected ResourceDto load() {
                return loadResourceDto();
            }
        };
        initLayout();
    }

    private ResourceDto loadResourceDto() {
        if (!isResourceOidAvailable()) {
            getSession().error(getString("pageResource.message.oidNotDefined"));
            throw new RestartResponseException(PageResources.class);
        }

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(ResourceType.F_CONNECTOR, GetOperationOptions.createResolve());

        resource = loadResource(options);
        return new ResourceDto(resource, getPrismContext(), resource.asObjectable().getConnector(),
                initCapabilities(resource.asObjectable()));
    }

    private void initLayout() {
        Form mainForm = new Form(ID_FORM_MAIN);
        add(mainForm);

        SortableDataProvider<ResourceObjectTypeDto, String> provider = new ListDataProvider<>(this,
                new PropertyModel<List<ResourceObjectTypeDto>>(model, "objectTypes"));
        provider.setSort("displayName", SortOrder.ASCENDING);
        TablePanel objectTypes = new TablePanel<>(ID_TABLE_OBJECT_TYPE, provider, initObjectTypesColumns(),
                UserProfileStorage.TableId.PAGE_RESOURCE_PANEL, getItemsPerPage(UserProfileStorage.TableId.PAGE_RESOURCE_PANEL));
        objectTypes.setShowPaging(true);
        objectTypes.setOutputMarkupId(true);
        mainForm.add(objectTypes);

        initResourceColumns(mainForm);
        initConnectorDetails(mainForm);
        createCapabilitiesList(mainForm);

        initButtons(mainForm);
    }

    private void initResourceColumns(Form mainForm) {
        mainForm.add(new Label(ID_RESOURCE_OID, new PropertyModel<>(model, "oid")));
        mainForm.add(new Label(ID_RESOURCE_NAME, new PropertyModel<>(model, "name")));
        mainForm.add(new Label(ID_RESOURCE_TYPE, new PropertyModel<>(model, "type")));
        mainForm.add(new Label(ID_RESOURCE_VERSION, new PropertyModel<>(model, "version")));
        mainForm.add(new Label(ID_RESOURCE_PROGRESS, new PropertyModel<>(model, "progress")));
    }

    private IModel<String> createTestConnectionStateTooltip(final String expression) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PropertyModel<OperationResultStatus> pModel = new PropertyModel<OperationResultStatus>(model, expression);
                OperationResultStatus status = pModel.getObject();
                if (status == null) {
                    return "";
                }

                return PageResource.this.getString(OperationResultStatus.class.getSimpleName() + "." + status.name());
            }
        };
    }

    private Label createImageLabel(String id, IModel<String> title, IModel<String> cssClass) {
        Label label = new Label(id);
        label.add(AttributeModifier.replace("class", cssClass));
        label.add(AttributeModifier.replace("title", title));

        return label;
    }

    private void initConnectorDetails(Form mainForm) {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONNECTORS);
        container.setOutputMarkupId(true);
        mainForm.add(container);

        container.add(createImageLabel(ID_OVERALL_STATUS, createTestConnectionStateTooltip("state.overall"),
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return OperationResultStatusIcon.parseOperationalResultStatus(model.getObject().getState().getOverall()).getIcon();
                    }
                }
        ));
        container.add(createImageLabel(ID_CONF_VALIDATION, createTestConnectionStateTooltip("state.confValidation"),
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return OperationResultStatusIcon.parseOperationalResultStatus(model.getObject().getState().getConfValidation()).getIcon();
                    }
                }
        ));
        container.add(createImageLabel(ID_CON_INITIALIZATION, createTestConnectionStateTooltip("state.conInitialization"),
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return OperationResultStatusIcon.parseOperationalResultStatus(model.getObject().getState().getConInitialization()).getIcon();
                    }
                }
        ));
        container.add(createImageLabel(ID_CON_CONNECTION, createTestConnectionStateTooltip("state.conConnection"),
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return OperationResultStatusIcon.parseOperationalResultStatus(model.getObject().getState().getConConnection()).getIcon();
                    }
                }
        ));

        container.add(createImageLabel(ID_CON_SCHEMA, createTestConnectionStateTooltip("state.conSchema"),
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return OperationResultStatusIcon.parseOperationalResultStatus(model.getObject().getState().getConSchema()).getIcon();
                    }
                }
        ));
    }

    private List<String> initCapabilities(ResourceType resource) {
        OperationResult result = new OperationResult("Load resource capabilities");
        List<String> capabilitiesName = new ArrayList<>();
        try {
            List<Object> capabilitiesList = ResourceTypeUtil.getEffectiveCapabilities(resource);

            if (capabilitiesList != null && !capabilitiesList.isEmpty()) {
                for (int i = 0; i < capabilitiesList.size(); i++) {
                    capabilitiesName.add(getCapabilityName(capabilitiesList.get(i)));
                }
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't load resource capabilities for resource'"
                    + new PropertyModel<>(model, "name") + ".", ex);

        }
        return capabilitiesName;
    }

    private String getCapabilityName(Object capability) {
        if (capability instanceof JAXBElement) {
            capability = ((JAXBElement) capability).getValue();
        }

        StringBuilder sb = new StringBuilder();

        String className = capability.getClass().getSimpleName();
        if (className.endsWith("CapabilityType")) {
            sb.append(className.substring(0, className.length() - "CapabilityType".length()));
        } else {
            sb.append(className);
        }

        if (capability instanceof ScriptCapabilityType) {
            ScriptCapabilityType script = (ScriptCapabilityType) capability;
            sb.append(": ");
            List<ProvisioningScriptHostType> hosts = new ArrayList<>();
            for (ScriptCapabilityType.Host host : script.getHost()) {
                hosts.add(host.getType());
            }

            sb.append(StringUtils.join(hosts, ", "));
        }

        return sb.toString();
    }

    private List<IColumn<ResourceObjectTypeDto, String>> initObjectTypesColumns() {
        List<IColumn<ResourceObjectTypeDto, String>> columns = new ArrayList<IColumn<ResourceObjectTypeDto, String>>();

        columns.add(new PropertyColumn(createStringResource("pageResource.objectTypes.displayName"),
                "displayName", "displayName"));
        columns.add(new PropertyColumn(createStringResource("pageResource.objectTypes.nativeObjectClass"),
                "nativeObjectClass"));
        columns.add(new PropertyColumn(createStringResource("pageResource.objectTypes.help"), "help"));
        columns.add(new PropertyColumn(createStringResource("pageResource.objectTypes.type"), "type"));

        return columns;
    }

    private void createCapabilitiesList(Form mainForm) {
        ListView<String> listCapabilities = new ListView<String>(ID_LIST_CAPABILITIES, createCapabilitiesModel(model)) {

            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(ID_CAPABILITIES, item.getModel()));

            }
        };
        mainForm.add(listCapabilities);
    }

    private IModel<List<String>> createCapabilitiesModel(final IModel<ResourceDto> model) {
        return new LoadableModel<List<String>>(false) {

            @Override
            protected List<String> load() {
                ResourceDto resource = model.getObject();
                return resource.getCapabilities();
            }
        };
    }

    private void initButtons(Form mainForm) {
        AjaxButton back = new AjaxButton(ID_BUTTON_BACK, createStringResource("pageResource.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getPreviousPage() != null) {
                    goBack(PageDashboard.class);            // the parameter is never used really
                } else {
                    setResponsePage(new PageResources(false));
                }
            }
        };
        mainForm.add(back);

        AjaxButton test = new AjaxButton(ID_BUTTON_TEST, createStringResource("pageResource.button.test")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testConnectionPerformed(target);
            }
        };
        mainForm.add(test);

        AjaxButton importAccounts = new AjaxButton(ID_BUTTON_IMPORT_ACCOUNTS,
                createStringResource("pageResource.button.importAccounts")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                importFromResourcePerformed(target);
            }
        };
        mainForm.add(importAccounts);

        AjaxButton link = new AjaxButton(ID_BUTTON_EDIT, createStringResource("pageResource.editResource")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, model.getObject().getOid());
                setResponsePage(new PageResourceWizard(parameters));
            }
        };
        mainForm.add(link);

        AjaxButton deleteToken = new AjaxButton(ID_BUTTON_DELETE_SYNC_TOKEN, createStringResource("pageResource.deleteSyncToken")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteSyncTokenPerformed(target, model);
            }
        };
        mainForm.add(deleteToken);
    }

    private void testConnectionPerformed(AjaxRequestTarget target) {
        ResourceDto dto = model.getObject();
        if (dto == null || StringUtils.isEmpty(dto.getOid())) {
            error(getString("pageResource.message.oidNotDefined"));
            target.add(getFeedbackPanel());
            return;
        }

        Task task = createSimpleTask(TEST_CONNECTION);
        OperationResult result = new OperationResult(TEST_CONNECTION);
        try {
            result = getModelService().testResource(dto.getOid(), task);
            ResourceController.updateResourceState(dto.getState(), result);

            // this provides some additional tests, namely a test for schema handling section
            getModelService().getObject(ResourceType.class, dto.getOid(), null, task, result);
        } catch (ObjectNotFoundException ex) {
            result.recordFatalError("Failed to test resource connection", ex);
        } catch (ConfigurationException e) {
            result.recordFatalError("Failed to test resource connection", e);
        } catch (SchemaException e) {
            result.recordFatalError("Failed to test resource connection", e);
        } catch (CommunicationException e) {
            result.recordFatalError("Failed to test resource connection", e);
        } catch (SecurityViolationException e) {
            result.recordFatalError("Failed to test resource connection", e);
        }

        // a bit of hack: result of TestConnection contains a result of getObject as a subresult
        // so in case of TestConnection succeeding we recompute the result to show any (potential) getObject problems
        if (result.isSuccess()) {
            result.recomputeStatus();
        }

        WebMarkupContainer connectors = (WebMarkupContainer) get(ID_FORM_MAIN + ":" + ID_CONNECTORS);
        target.add(connectors);

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    private void importFromResourcePerformed(AjaxRequestTarget target) {
        ResourceDto dto = model.getObject();
        LOGGER.debug("Import accounts from resource {} ({}), object class {}",
                new Object[]{dto.getName(), dto.getOid(), dto.getDefaultAccountObjectClass()});

        OperationResult result = new OperationResult(OPERATION_IMPORT_FROM_RESOURCE);
        try {
            Task task = createSimpleTask(OPERATION_IMPORT_FROM_RESOURCE);
            QName objectClass =  dto.getDefaultAccountObjectClass();
            if (objectClass == null){
                RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
                        LayerType.MODEL, getPrismContext());
                if (refinedSchema == null) {
                    error("No refined schema for " + resource);
                    LOGGER.debug("No refined schema for " + resource);
                } else {
                    objectClass = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT).getObjectClassDefinition()
                            .getTypeName();
                }

            }
            getModelService().importFromResource(dto.getOid(), objectClass, task, result);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Error occurred during accounts import from resource {} ({}), class {}",
                    ex, dto.getName(), dto.getOid(), dto.getDefaultAccountObjectClass());
            result.recordFatalError("Error occurred during importing accounts from resource.", ex);
        }

        result.computeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
    }
}
