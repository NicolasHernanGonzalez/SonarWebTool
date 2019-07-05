package ar.com.globallogic.sonarWebTool.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import ar.com.globallogic.sonarWebTool.dtos.ProjectMetricDTO;
import ar.com.globallogic.sonarWebTool.exceptions.BusinessException;
import ar.com.globallogic.sonarWebTool.services.IMetricService;

import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

@Controller
public class MainController {

	@Autowired
	private IMetricService metricService;
	
	@Value("${google.user}")
	private String googleUser;
	
	@Value("${google.password}")
	private String googlePassword;
	
	@Value("${google.spreadSheet.name}")
	private String defaultSpreadSheetName;
	
	@Value("${google.spreadSheet.dateColumn.name}")
	private String dateSpreadSheetColumnName;
	
	@Value("${google.spreadSheet.projectColumn.name}")
	private String projectSpreadSheetColumnName;
	
	@Value("${google.spreadSheet.metricColumn.name}")
	private String metricSpreadSheetColumnName;

	@Value("${google.spreadSheet.valueColumn.name}")
	private String valueSpreadSheetColumnName;
	
	
	private static Logger logger = Logger.getLogger(MainController.class);
	
	
	@ExceptionHandler(BusinessException.class)
	@ResponseStatus( value = HttpStatus.INTERNAL_SERVER_ERROR )
	@ResponseBody
	public String handleException(Exception ex, HttpServletResponse response) {

		logger.error("Handlng Exception : " + ex.getClass().getSimpleName());
		logger.error(ex.getMessage());
		
		response.setContentType("application/json");

		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(ex.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error cathing exception :" + ex.getClass());
			return e.getMessage(); 
		}
	}
	
	/**
	 * 
	 * @param projectKey, the key of the project (sonar project key) which metrics will be exported. If null, all projects metrics will be exported  
	 * @param sonnarServerUrl, Sonar web server host. If null the default (generalConf.properties) will be used 
	 * @param sonarUser, Sonar web server user. (optional)
	 * @param sonarPassword, Sonar web server password.(optional)
	 * @param sheetName,  Name of the google sheet to export project 's metrics 
	 * @param metricKeys, Keys of the metric to use. If null, the default (generalConf.properties) will be used.
	 * @return
	 * @throws BusinessException 
	 */
	@RequestMapping(value = "/getMetricInfo" , method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String executeMetric(@RequestParam(value = "projectKey",required = false) String projectKey,
								@RequestParam(value = "sonnarServerUrl",required = false) String sonnarServerUrl,
								@RequestParam(value = "sonarUser",required = false) String sonarUser,
								@RequestParam(value = "sonarPassword",required = false) String sonarPassword,
								@RequestParam(value = "sheetName", required = false) String sheetName,
								@RequestParam(value = "metricKeys", required = false) String... metricKeys) {
		
		List<ProjectMetricDTO> dtos = metricService.getMetricInfo(sonnarServerUrl, sonarUser, sonarPassword, projectKey, metricKeys);
		
		logger.info("Exporting project metrics results...");
		
		String spreadSheet = chooseSpreadSheet(sheetName);
		
		exportprojectMetrics(dtos,spreadSheet);
		
		logger.info("Success..");
		logger.info("Success..");
		logger.info("Success..");
		
		return "Success! ";
	}
	
	protected String chooseSpreadSheet(String sheetName) {
		
		if (StringUtils.hasText(sheetName)) {
			return sheetName;
		}
		
		logger.info("SheetName parameter not received, the deafault will be used : '" + defaultSpreadSheetName + "'");
		
		return defaultSpreadSheetName;
	}

	private void exportprojectMetrics(List<ProjectMetricDTO> dtos,String spreadSheetName) {

		FeedURLFactory factory = FeedURLFactory.getDefault();
		
		try {
			
			logger.info("Verifying credentials...");
			SpreadsheetService service = verifyCredentials();
			
			SpreadsheetFeed spreadsheetFeed = getSpreadSheetFeed(factory, service,spreadSheetName);
			
			List<SpreadsheetEntry> spreadsheets = spreadsheetFeed.getEntries();
			
			if (spreadsheets.isEmpty()) {
				logger.error("No spreadsheets with the name : '" + spreadSheetName + "'");
				throw new BusinessException("Error : No spreadsheets with the name : '" + spreadSheetName + "'");
			}
			
			SpreadsheetEntry entry = spreadsheets.get(0);
			
			logger.info("Using '" + entry.getTitle().getPlainText() + "' spreadsheet");
			
			URL listFeedUrl = getListFeedUrl(service, entry);
			
			insertprojectsMetricsInfo(dtos, service, listFeedUrl);
			
		} 
		catch (AuthenticationException e) {
			logger.error("Invalid User credentials)");
			throw new BusinessException("Invalid User credentials",e);
		}
		catch (Exception e) {
			logger.error("Error trying to export metrics to google spreadsheet (" + spreadSheetName + ")");
			throw new BusinessException("Error trying to export metrics to google spreadsheet",e);
		}
	}

	protected void insertprojectsMetricsInfo(List<ProjectMetricDTO> dtos, SpreadsheetService service, URL listFeedUrl) throws IOException, ServiceException {
		ListEntry row = new ListEntry();
		
		logger.debug("Building spreadsheet rows");
		
		for (ProjectMetricDTO dto : dtos) {
			row.getCustomElements().setValueLocal(dateSpreadSheetColumnName,dto.getDate());
			row.getCustomElements().setValueLocal(projectSpreadSheetColumnName, dto.getprojectName());
			row.getCustomElements().setValueLocal(metricSpreadSheetColumnName, dto.getMetricName());
			row.getCustomElements().setValueLocal(valueSpreadSheetColumnName, formatMetricValue(dto.getMetricValue()));
			
			logger.debug("adding spreadsheet rows");
			row = service.insert(listFeedUrl, row);
		}
	}
	
	private String formatMetricValue(String metricValue){
		return metricValue.replace(".",",").trim();
	}

	private URL getListFeedUrl(SpreadsheetService service, SpreadsheetEntry entry) throws IOException, ServiceException {
		WorksheetFeed worksheetFeed = service.getFeed(entry.getWorksheetFeedUrl(), WorksheetFeed.class);
		List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
		WorksheetEntry worksheet = worksheets.get(0);
		return worksheet.getListFeedUrl();
	}

	private SpreadsheetFeed getSpreadSheetFeed(FeedURLFactory factory, SpreadsheetService service,String spreadSheetName) throws IOException, ServiceException {
		SpreadsheetQuery spreadsheetQuery = new SpreadsheetQuery(factory.getSpreadsheetsFeedUrl());
		spreadsheetQuery.setTitleQuery(spreadSheetName);
		spreadsheetQuery.setTitleExact(true);
		return service.query(spreadsheetQuery, SpreadsheetFeed.class);
	}

	private SpreadsheetService verifyCredentials() throws AuthenticationException {
		SpreadsheetService service = new SpreadsheetService("metricSpreadSheet");
		service.setUserCredentials(googleUser, googlePassword);
		return service;
	}
}