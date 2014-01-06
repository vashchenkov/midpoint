/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.report;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PagingConvertor;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrientationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportParameterConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportTemplateStyleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import com.evolveum.midpoint.report.ReportCreateTaskHandler;
import com.evolveum.midpoint.report.ReportManager;

/**
 * @author garbika
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BasicReportTest extends AbstractModelIntegrationTest {
    
	private static final String CLASS_NAME_WITH_DOT = BasicReportTest.class
			.getName() + ".";
	private static final String GET_REPORT = CLASS_NAME_WITH_DOT
			+ "getReport";
	private static final String GET_REPORT_OUTPUT = CLASS_NAME_WITH_DOT
			+ "getReportOutput";
	private static final String IMPORT_USERS = CLASS_NAME_WITH_DOT
			+ "importUsers";
	private static final String CREATE_REPORT = CLASS_NAME_WITH_DOT
			+ "test001CreateReport";
	private static final String CREATE_REPORT_FROM_FILE = CLASS_NAME_WITH_DOT
			+ "test002CreateReportFromFile";
	private static final String COPY_REPORT_WITHOUT_DESIGN = CLASS_NAME_WITH_DOT
			+ "test003CopyReportWithoutDesign";
	private static final String RUN_REPORT = CLASS_NAME_WITH_DOT
			+ "test004RunReport";
	private static final String PARSE_OUTPUT_REPORT = CLASS_NAME_WITH_DOT
			+ "test005ParseOutputReport";
	private static final String RUN_TASK = CLASS_NAME_WITH_DOT
			+ "test006RunTask";
	private static final String COUNT_REPORT = CLASS_NAME_WITH_DOT
			+ "test007CountReport";
	private static final String SEARCH_REPORT = CLASS_NAME_WITH_DOT
			+ "test008SearchReport";
	private static final String MODIFY_REPORT = CLASS_NAME_WITH_DOT
			+ "test009ModifyReport";
	private static final String DELETE_REPORT = CLASS_NAME_WITH_DOT
			+ "test010DeleteReport";
	private static final Trace LOGGER = TraceManager
			.getTrace(BasicReportTest.class);

	private static final File REPORTS_DIR = new File("src/test/resources/reports");
	private static final File STYLES_DIR = new File("src/test/resources/styles");
	private static final File COMMON_DIR = new File("src/test/resources/common");
	
	private static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR + "/system-configuration.xml");
	private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR + "/user-administrator.xml");
	private static final File TASK_REPORT_FILE = new File(COMMON_DIR + "/task-report.xml");  
	private static final File TEST_REPORT_WITHOUT_DESIGN_FILE = new File(REPORTS_DIR + "/report-test-without-design.xml");
	private static final File TEST_REPORT_FILE = new File(REPORTS_DIR + "/report-test.xml");
	private static final File REPORT_DATASOURCE_TEST = new File(REPORTS_DIR + "/reportDataSourceTest.jrxml");
	private static final File STYLE_TEMPLATE_DEFAULT = new File(STYLES_DIR	+ "/midpoint_base_styles.jrtx");
	
	private static final String REPORT_OID_001 = "00000000-3333-3333-0000-100000000001";
	private static final String REPORT_OID_002 = "00000000-3333-3333-0000-100000000002";
	private static final String REPORT_OID_TEST = "00000000-3333-3333-TEST-10000000000";
	private static final String TASK_REPORT_OID = "00000000-3333-3333-TASK-10000000000";
	
	@Autowired
	private PrismContext prismContext;

	@Autowired
	private ModelService modelService;
	
	@Autowired
	private ReportManager reportManager;
	
	@Autowired
	private ReportCreateTaskHandler reportHandler;
	
	protected PrismObject<UserType> userAdministrator;
	
	public BasicReportTest() {
		super();
	}
	
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
		
		// Users
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult);
	}
	
	protected Task createTask(String operationName) {
		Task task = taskManager.createTaskInstance(operationName);
		task.setOwner(userAdministrator);
		return task;
	}
	
	private PrismObject<ReportType> getReport(String reportOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(GET_REPORT);
        OperationResult result = task.getResult();
		PrismObject<ReportType> report = modelService.getObject(ReportType.class, reportOid, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess("getObject(Report) result not success", result);
		return report;
	}
	
	private List<PrismObject<ReportOutputType>> searchReportOutput(String reportOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(GET_REPORT_OUTPUT);
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
        ObjectFilter filter = RefFilter.createReferenceEqual(ReportOutputType.class, ReportOutputType.F_REPORT_REF, prismContext, reportOid);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		List<PrismObject<ReportOutputType>> reportOutputList = modelService.searchObjects(ReportOutputType.class, query, options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess("getObject(Report) result not success", result);
		return reportOutputList;
	}

	private void importUsers(int from, int to) throws Exception {
		Task task = taskManager.createTaskInstance(IMPORT_USERS);
        OperationResult result = task.getResult();
        OperationResult subResult = null;
		for (int i=from; i<=to; i++)
		{
			UserType user = new UserType();
			prismContext.adopt(user);
			user.setName(new PolyStringType("User_" + Integer.toString(i)));
			ObjectDelta<UserType> objectDelta = ObjectDelta.createAddDelta((PrismObject<UserType>) user.asPrismObject());
			Collection<ObjectDelta<? extends ObjectType>> deltas =	MiscSchemaUtil.createCollection(objectDelta);
			LOGGER.trace("insert user {}", user);
			subResult = result.createSubresult("User : " + user.getName().getOrig());
			modelService.executeChanges(deltas, null, task, subResult);
			subResult.computeStatus();
			TestUtil.assertSuccess("import user result not success", subResult);
		}
		
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
		subResult = result.createSubresult("count users");
		int countUsers = modelService.countObjects(UserType.class, null, options, task, subResult);
		LOGGER.trace("count users {}: ", countUsers);
		assertEquals("Unexpected number of count users", to + 1, countUsers);
		result.computeStatus();
	}
	
	@Test
	public void test001CreateReport() throws Exception {
		// import vo for cycle cez usertype zrusit vsetky RTable

		final String TEST_NAME = "test001CreateReport";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        ReportType reportType = new ReportType();
		prismContext.adopt(reportType);
		
		reportType.setOid(REPORT_OID_001);

		// description and name
		reportType.setName(new PolyStringType("Test report - Datasource1"));
		reportType.setDescription("TEST Report with DataSource parameter.");

		// file templates
		String jrxmlFile = FileUtils.readFileToString(REPORT_DATASOURCE_TEST, "UTF-8");// readFile(REPORT_DATASOURCE_TEST, StandardCharsets.UTF_8);
		ReportTemplateType reportTemplate = new ReportTemplateType();
		reportTemplate.setAny(DOMUtil.parseDocument(jrxmlFile).getDocumentElement());
		reportType.setReportTemplate(reportTemplate);

		String jrtxFile = FileUtils.readFileToString(STYLE_TEMPLATE_DEFAULT, "UTF-8"); //readFile(STYLE_TEMPLATE_DEFAULT, StandardCharsets.UTF_8);
		ReportTemplateStyleType reportTemplateStyle = new ReportTemplateStyleType();
		reportTemplateStyle.setAny(DOMUtil.parseDocument(jrtxFile).getDocumentElement());
		reportType.setReportTemplateStyle(reportTemplateStyle);

		// orientation
		reportType.setReportOrientation(OrientationType.LANDSCAPE);

		// export
		reportType.setReportExport(ExportType.PDF);

		// object class
		reportType.setObjectClass(ObjectTypes.getObjectType(UserType.class)
				.getTypeQName());

		// object query
		ObjectPaging paging = ObjectPaging.createPaging(0, 10);
		ObjectQuery query = ObjectQuery.createObjectQuery(paging);
		QueryType queryType = new QueryType();
		try {
			queryType = QueryConvertor.createQueryType(query, prismContext);
		} catch (Exception ex) {
			LOGGER.error("Exception occurred. QueryType", ex);
		}
		try {
			queryType.setPaging(PagingConvertor.createPagingType(query
					.getPaging()));
		} catch (Exception ex) {
			LOGGER.error("Exception occurred. QueryType pagging", ex);
		}
		reportType.setQuery(queryType);

		// fields
		List<ReportFieldConfigurationType> reportFields = new ArrayList<ReportFieldConfigurationType>();

		ReportFieldConfigurationType field = new ReportFieldConfigurationType();
		ItemPath itemPath;
		XPathHolder xpath;
		Element element;
		ItemDefinition itemDef;
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		PrismObjectDefinition<UserType> userDefinition = schemaRegistry
				.findObjectDefinitionByCompileTimeClass(UserType.class);

		field.setNameHeaderField("Name");
		field.setNameReportField("Name");
		itemPath = new ItemPath(UserType.F_NAME);
		xpath = new XPathHolder(itemPath);
		element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
				DOMUtil.getDocument());// (SchemaConstantsGenerated.NS_COMMON,
										// "itemPathField");
		field.setItemPathField(element);
		field.setSortOrderNumber(1);
		field.setSortOrder(OrderDirectionType.ASCENDING);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidthField(25);
		field.setClassTypeField(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeaderField("First Name");
		field.setNameReportField("FirstName");
		itemPath = new ItemPath(UserType.F_GIVEN_NAME);
		xpath = new XPathHolder(itemPath);
		element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
				DOMUtil.getDocument());
		field.setItemPathField(element);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidthField(25);
		field.setClassTypeField(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeaderField("Last Name");
		field.setNameReportField("LastName");
		itemPath = new ItemPath(UserType.F_FAMILY_NAME);
		xpath = new XPathHolder(itemPath);
		element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
				DOMUtil.getDocument());
		field.setItemPathField(element);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidthField(25);
		field.setClassTypeField(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeaderField("Activation");
		field.setNameReportField("Activation");
		itemPath = new ItemPath(UserType.F_ACTIVATION,
				ActivationType.F_ADMINISTRATIVE_STATUS);
		xpath = new XPathHolder(itemPath);
		element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
				DOMUtil.getDocument());
		field.setItemPathField(element);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidthField(25);
		field.setClassTypeField(itemDef.getTypeName());
		reportFields.add(field);

		reportType.getReportField().addAll(reportFields);

		// parameters
		List<ReportParameterConfigurationType> reportParameters = new ArrayList<ReportParameterConfigurationType>();

		ReportParameterConfigurationType parameter = new ReportParameterConfigurationType();
		parameter.setNameParameter("LOGO_PATH");
		parameter.setValueParameter(REPORTS_DIR.getPath() + "/logo.jpg");
		parameter.setClassTypeParameter(DOMUtil.XSD_ANY);
		reportParameters.add(parameter);

		parameter = new ReportParameterConfigurationType();
		parameter.setNameParameter("BaseTemplateStyles");
		parameter.setValueParameter(STYLE_TEMPLATE_DEFAULT.getPath());
		parameter.setClassTypeParameter(DOMUtil.XSD_STRING);
		reportParameters.add(parameter);

		reportType.getReportParameter().addAll(reportParameters);

		Task task = taskManager.createTaskInstance(CREATE_REPORT);
		OperationResult result = task.getResult();
		result.addParams(new String[] { "task" }, task.getResult());
		result.addParams(new String[] { "object" }, reportType);

		ObjectDelta<ReportType> objectDelta = ObjectDelta
				.createAddDelta(reportType.asPrismObject());
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil
				.createCollection(objectDelta);
		
		//WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
		AssertJUnit.assertEquals(REPORT_OID_001, objectDelta.getOid());

		reportType = getReport(REPORT_OID_001).asObjectable();

		// export xml structure of report type
		/*String xmlReportType = prismContext.getPrismDomProcessor()
				.serializeObjectToString(reportType.asPrismObject());
		LOGGER.warn(xmlReportType);
*/
		// check reportType
		AssertJUnit.assertNotNull(reportType);
		AssertJUnit.assertEquals("Test report - Datasource1", reportType
				.getName().getOrig());
		AssertJUnit.assertEquals("TEST Report with DataSource parameter.",
				reportType.getDescription());
		
		/*
		LOGGER.warn("reportTemplate orig ::::::::: " + DOMUtil.serializeDOMToString((Node)reportTemplate.getAny()));
		LOGGER.warn("reportTemplate DB ::::::::: " + DOMUtil.serializeDOMToString((Node)reportType.getReportTemplate().getAny()));
		
		LOGGER.warn("reportTemplateStyle orig ::::::::: " + DOMUtil.serializeDOMToString((Node)reportTemplateStyle.getAny()));
		LOGGER.warn("reportTemplateStyle DB ::::::::: " + DOMUtil.serializeDOMToString((Node)reportType.getReportTemplateStyle().getAny()));
		
		
		String reportTemplateRepoString = DOMUtil.serializeDOMToString((Node)reportType.getReportTemplate().getAny());
   	 	InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplateRepoString.getBytes());
   	 	JasperDesign jasperDesignRepo = JRXmlLoader.load(inputStreamJRXML);
   	 	
   	 	String reportTemplateString = DOMUtil.serializeDOMToString((Node)reportTemplate.getAny());
	 	inputStreamJRXML = new ByteArrayInputStream(reportTemplateString.getBytes());
	 	JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML);
	 	*/
	 	//AssertJUnit.assertEquals(jasperDesign, jasperDesignRepo);
		//AssertJUnit.assertEquals(reportTemplateStyle.getAny(), reportType.getReportTemplateStyle().getAny());
		AssertJUnit.assertEquals(OrientationType.LANDSCAPE,
				reportType.getReportOrientation());
		AssertJUnit.assertEquals(ExportType.PDF, reportType.getReportExport());
		AssertJUnit.assertEquals(ObjectTypes.getObjectType(UserType.class)
				.getTypeQName(), reportType.getObjectClass());
		AssertJUnit.assertEquals(queryType, reportType.getQuery());

		int fieldCount = reportFields.size();
		List<ReportFieldConfigurationType> fieldsRepo = reportType
				.getReportField();

		ReportFieldConfigurationType fieldRepo = null;
		AssertJUnit.assertEquals(fieldCount, fieldsRepo.size());
		for (int i = 0; i < fieldCount; i++) {
			fieldRepo = fieldsRepo.get(i);
			field = reportFields.get(i);
			AssertJUnit.assertEquals(field.getNameHeaderField(),
					fieldRepo.getNameHeaderField());
			AssertJUnit.assertEquals(field.getNameReportField(),
					fieldRepo.getNameReportField());
			ItemPath fieldPath = new XPathHolder(field.getItemPathField())
					.toItemPath();
			ItemPath fieldRepoPath = new XPathHolder(
					fieldRepo.getItemPathField()).toItemPath();
			AssertJUnit.assertEquals(fieldPath, fieldRepoPath);
			AssertJUnit.assertEquals(field.getSortOrder(),
					fieldRepo.getSortOrder());
			AssertJUnit.assertEquals(field.getSortOrderNumber(),
					fieldRepo.getSortOrderNumber());
			AssertJUnit.assertEquals(field.getWidthField(),
					fieldRepo.getWidthField());
			AssertJUnit.assertEquals(field.getClassTypeField(),
					fieldRepo.getClassTypeField());
		}

		int parameterCount = reportParameters.size();
		List<ReportParameterConfigurationType> parametersRepo = reportType
				.getReportParameter();

		ReportParameterConfigurationType parameterRepo = null;
		AssertJUnit.assertEquals(parameterCount, parametersRepo.size());
		for (int i = 0; i < parameterCount; i++) {
			parameterRepo = parametersRepo.get(i);
			parameter = reportParameters.get(i);
			AssertJUnit.assertEquals(parameter.getNameParameter(),
					parameterRepo.getNameParameter());
			AssertJUnit.assertEquals(parameter.getValueParameter(),
					parameterRepo.getValueParameter());
			AssertJUnit.assertEquals(parameter.getClassTypeParameter(),
					parameterRepo.getClassTypeParameter());
		}
	}
	
	@Test 
	public void test002CreateReportFromFile() throws Exception {
		
		final String TEST_NAME = "test002CreateReportFromFile";
        TestUtil.displayTestTile(this, TEST_NAME);
	
		Task task = taskManager.createTaskInstance(CREATE_REPORT_FROM_FILE);
		OperationResult result = task.getResult();
		 
		PrismObject<? extends Objectable> reportType =  prismContext.getPrismDomProcessor().parseObject(TEST_REPORT_FILE);
			 
		ObjectDelta<ReportType> objectDelta = 
		ObjectDelta.createAddDelta((PrismObject<ReportType>) reportType);
		Collection<ObjectDelta<? extends ObjectType>> deltas =
		MiscSchemaUtil.createCollection(objectDelta);
		
		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		//THEN  
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
		AssertJUnit.assertEquals(REPORT_OID_TEST, objectDelta.getOid());
		
		
	}
	@Test
	public void test003CopyReportWithoutDesign() throws Exception {
		final String TEST_NAME = "test003CopyReportWithoutDesign";
        TestUtil.displayTestTile(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(COPY_REPORT_WITHOUT_DESIGN);
		OperationResult result = task.getResult();

		ReportType reportType = getReport(REPORT_OID_001).asObjectable();

		reportType = reportType.clone();
		reportType.setOid(REPORT_OID_002);
		reportType.setReportTemplate(null);
		reportType.setReportTemplateStyle(null);
		reportType.setName(new PolyStringType("Test report - Datasource2"));

		ObjectDelta<ReportType> objectDelta = ObjectDelta
				.createAddDelta(reportType.asPrismObject());
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil
				.createCollection(objectDelta);
		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		//THEN  
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);
		AssertJUnit.assertEquals(REPORT_OID_002, objectDelta.getOid());
	}
	
	@Test
	public void test004RunReport() throws Exception {
		final String TEST_NAME = "test004RunReport";
        TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
        Task task = createTask(RUN_REPORT);
		OperationResult result = task.getResult();
		importUsers(1,10);
		ReportType reportType = getReport(REPORT_OID_TEST).asObjectable();
		
		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		reportManager.runReport(reportType.asPrismObject(), task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
        //OperationResult subresult = result.getLastSubresult();
        //TestUtil.assertInProgress("create report result", subresult);
        
        waitForTaskFinish(task.getOid(), false);
        
     // Task result
        PrismObject<TaskType> reportTaskAfter = getTask(task.getOid());
        OperationResultType reportTaskResult = reportTaskAfter.asObjectable().getResult();
        display("Report task result", reportTaskResult);
        TestUtil.assertSuccess(reportTaskResult);
	}

	@Test
	public void test005ParseOutputReport() throws Exception {
		final String TEST_NAME = "test005ParseOutputReport";
        TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
        Task task = createTask(PARSE_OUTPUT_REPORT);
		OperationResult result = task.getResult();
		
        ReportOutputType reportOutputType = searchReportOutput(REPORT_OID_TEST).get(0).asObjectable();
        LOGGER.trace("read report output {}", reportOutputType);
        
        ReportType reportType = getReport(REPORT_OID_TEST).asObjectable();
        LOGGER.trace("read report {}", reportType);
        
        String output = ReportUtils.getReportOutputFilePath(reportType);
        
        AssertJUnit.assertNotNull(reportOutputType);
        assertEquals("Unexpected report reference", MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE), reportOutputType.getReportRef());
        assertEquals("Unexpected report file path", output, reportOutputType.getReportFilePath());
       
        BufferedReader br = null;  
        String line = "";  
        String splitBy = ",";  
        
        br = new BufferedReader(new FileReader(reportOutputType.getReportFilePath()));  
        int count = 0;
        while ((line = br.readLine()) != null) {  
        	count++;
        	String[] lineDetails = line.split(splitBy); 
        	switch (count)
        	{
        		case 1:  assertEquals("Unexpected name of report", "User Report", lineDetails[3]);
        			break;
        		case 2:  assertEquals("Unexpected second line of report", "Report generated on:", lineDetails[7]);
    				break;
        		case 3:  {
        			assertEquals("Unexpected third line of report", "Number of records:", lineDetails[7]);
        			assertEquals("Unexpected number of records", 11, Integer.parseInt(lineDetails[8]));
        			}
    				break;
        		case 4: {
        			assertEquals("Unexpected column header of report - name", "Name", lineDetails[1]);
        			assertEquals("Unexpected column header of report - first name", "First name", lineDetails[4]);
        			assertEquals("Unexpected column header of report - last name", "Last name", lineDetails[5]);
        			assertEquals("Unexpected column header of report - activation", "Activation", lineDetails[6]);
        			}
    				break;
        		case 5:	LOGGER.trace("USERS [name= " + lineDetails[0] + " , first name=" + lineDetails[4] + " , last name=" + lineDetails[5] + " , activation=" + lineDetails[6] + "]");
        			break;
        		case 6:
        		case 7:
        		case 8:
        		case 9:
        		case 10:
        		case 11:
        		case 12:
        		case 13:
        		case 14:
        		case 15:
        			LOGGER.trace("USERS [name= " + lineDetails[0] + "]");
        			break;
        		case 16: {
        			assertEquals("Unexpected text", "Page 1 of", lineDetails[9]);
        			String pages = lineDetails[10].trim();
        			assertEquals("Unexpected count pages", 1, Integer.parseInt(pages));
        			}
        			break;
        		default: LOGGER.trace("incorrect]");
        			break;
        	}	
        }  
        if (br != null) br.close();  
        
        LOGGER.trace("Done with reading CSV");  
        assertEquals("Unexpected number of users", 11, count-5);
	}
	
	/*
	 * import report from xml file without design
	 * import task from xml file
	 * run task
	 * parse output file
	 */
	
	@Test
	public void test006RunTask() throws Exception {
		final String TEST_NAME = "test006RunTask";
        TestUtil.displayTestTile(this, TEST_NAME);
        boolean import10000 = false;
        int countUsers = 11;
        if (import10000){ 
        	importUsers(11, 10000);
        	countUsers = 10001;
        }
        	
        Task task = taskManager.createTaskInstance(RUN_TASK);
		OperationResult result = task.getResult();
		 
        PrismObject<? extends Objectable> reportType=  prismContext.getPrismDomProcessor().parseObject(TEST_REPORT_WITHOUT_DESIGN_FILE);
		 
		ObjectDelta<ReportType> objectDelta =
		ObjectDelta.createAddDelta((PrismObject<ReportType>) reportType);
		Collection<ObjectDelta<? extends ObjectType>> deltas =
		MiscSchemaUtil.createCollection(objectDelta);
		
		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		LOGGER.trace("import report task {}", TASK_REPORT_FILE.getPath());
        importObjectFromFile(TASK_REPORT_FILE);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        LOGGER.trace("run report task {}", TASK_REPORT_OID);
        waitForTaskFinish(TASK_REPORT_OID, false);
        
        // Task result
        PrismObject<TaskType> reportTaskAfter = getTask(TASK_REPORT_OID);
        OperationResultType reportTaskResult = reportTaskAfter.asObjectable().getResult();
        display("Report task result", reportTaskResult);
        TestUtil.assertSuccess(reportTaskResult);
        
        ReportOutputType reportOutputType = searchReportOutput(reportType.getOid()).get(0).asObjectable();
        LOGGER.trace("read report output {}", reportOutputType);
        
        ReportType report = getReport(reportType.getOid()).asObjectable();
        LOGGER.trace("read report {}", report);
        
        String output = ReportUtils.getReportOutputFilePath(report);
        
        AssertJUnit.assertNotNull(reportOutputType);
        assertEquals("Unexpected report reference", MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE), reportOutputType.getReportRef());
        assertEquals("Unexpected report file path", output, reportOutputType.getReportFilePath());
    /*       
        BufferedReader br = null;  
        String line = "";  
        String splitBy = ",";  
        
        br = new BufferedReader(new FileReader(reportOutputType.getReportFilePath()));  
        int count = 0;
        while ((line = br.readLine()) != null) {  
        	count++;
        	String[] lineDetails = line.split(splitBy); 
        	switch (count)
        	{
        		case 1:  assertEquals("Unexpected name of report", "DataSource Report", lineDetails[2]);
        			break;
        		case 2:  assertEquals("Unexpected second line of report", "Report generated on:", lineDetails[4]);
    				break;
        		case 3:  {
        			assertEquals("Unexpected third line of report", "Number of records:", lineDetails[4]);
        			assertEquals("Unexpected number of records", countUsers, Integer.parseInt(lineDetails[5]));
        			}
    				break;
        		case 4: {
        			assertEquals("Unexpected column header of report - name", "Name", lineDetails[0]);
        			assertEquals("Unexpected column header of report - first name", "First Name", lineDetails[3]);
        			assertEquals("Unexpected column header of report - last name", "Last Name", lineDetails[4]);
        			assertEquals("Unexpected column header of report - activation", "Activation", lineDetails[6]);
        			}
    				break;
        		case 5:	LOGGER.trace("USERS [name= " + lineDetails[0] + " , first name=" + lineDetails[3] + " , last name=" + lineDetails[4] + " , activation=" + lineDetails[6] + "]");
        			break;
        		case 16: {
        			assertEquals("Unexpected text", "Page 1 of", lineDetails[7]);
        			assertEquals("Unexpected count pages", 1, Integer.parseInt(lineDetails[8].replace("\\s", "")));
        			}
        			break;
        		default: LOGGER.trace("USERS [name= " + lineDetails[0] + "]");//LOGGER.trace("incorrect]");
        			break;
        	}	
        }  
        if (br != null) br.close();  
        
        LOGGER.trace("Done with reading CSV");  
        assertEquals("Unexpected number of users", countUsers, count-4);*/
	}


		@Test
		public void test007CountReport() throws Exception {
			final String TEST_NAME = "test007CountReport";
	        TestUtil.displayTestTile(this, TEST_NAME);

			Task task = taskManager.createTaskInstance(COUNT_REPORT);
			OperationResult result = task.getResult();

			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
					.createCollection(GetOperationOptions.createRaw());

			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			int count = modelService.countObjects(ReportType.class, null, options, task, result);
			
			//THEN
			TestUtil.displayThen(TEST_NAME);
	        result.computeStatus();
	        display(result);
	        TestUtil.assertSuccess(result);
	        assertEquals("Unexpected number of reports", 4, count);
		}

		@Test
		public void test008SearchReport() throws Exception {
			final String TEST_NAME = "test008SearchReport";
	        TestUtil.displayTestTile(this, TEST_NAME);

			Task task = taskManager.createTaskInstance(SEARCH_REPORT);
			OperationResult result = task.getResult();
			
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
					.createCollection(GetOperationOptions.createRaw());

			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			List<PrismObject<ReportType>> listReportType = modelService.searchObjects(ReportType.class, null, options, task, result);
			
			//THEN
			TestUtil.displayThen(TEST_NAME);
			result.computeStatus();
			display(result);
			TestUtil.assertSuccess(result);		
			assertEquals("Unexpected number of searching reports", 4, listReportType.size());
		}

		@Test
		public void test009ModifyReport() throws Exception {
			final String TEST_NAME = "test009ModifyReport";
	        TestUtil.displayTestTile(this, TEST_NAME);
			
			Task task = taskManager.createTaskInstance(MODIFY_REPORT);
			OperationResult result = task.getResult();

			ReportType reportType = getReport(REPORT_OID_001).asObjectable();

			reportType.setReportExport(ExportType.CSV);

			Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
			PrismObject<ReportType> reportTypeOld = modelService.getObject(
					ReportType.class, REPORT_OID_001, null, task, result);
			deltas.add(reportTypeOld.diff(reportType.asPrismObject()));

			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			modelService.executeChanges(deltas, null, task, result);
			
			//THEN
			TestUtil.displayThen(TEST_NAME);
			result.computeStatus();
			display(result);
			TestUtil.assertSuccess(result);	
			
			reportType = getReport(REPORT_OID_001).asObjectable();
			assertEquals("Unexpected export type", ExportType.CSV, reportType.getReportExport());
		}
		
		/**
		 * Delete report type.
		 */
		@Test
		public void test010DeleteReport() throws Exception {
			
			final String TEST_NAME = "test010DeleteReport";
	        TestUtil.displayTestTile(this, TEST_NAME);

			Task task = taskManager.createTaskInstance(DELETE_REPORT);
			OperationResult result = task.getResult();

			ObjectDelta<ReportType> delta = ObjectDelta.createDeleteDelta(ReportType.class, REPORT_OID_001, prismContext);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
			
			// WHEN
			modelService.executeChanges(deltas, null, task, result);
			
			// THEN
	        result.computeStatus();
	        TestUtil.assertSuccess(result);
			
			try {
	        	PrismObject<ReportType> report = getReport(REPORT_OID_001);
	        	AssertJUnit.fail("Report type was not deleted");
	        } catch (ObjectNotFoundException e) {
	        	// This is expected
	        }	
			
		}
		

}
