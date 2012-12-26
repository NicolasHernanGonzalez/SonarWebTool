package ar.com.globallogic.sonarWebTool.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.Times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ar.com.globallogic.sonarWebTool.dtos.ProjectMetricDTO;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.util.ServiceException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/application-contextTest.xml"})
@Configurable
public class MainControllerTest {
	
	@Value("${google.spreadSheet.name}")
	private String defaultSpreadSheetName;
	
	
	@Autowired
	private MainController mainController;

	@Test
	public void testChooseSpreadSheetWithValidSpreadSheetName() {
		String ValidSpreadSheetName = "ValidSpreadSheetName";
		String result = mainController.chooseSpreadSheet(ValidSpreadSheetName);
		assertEquals(result,ValidSpreadSheetName);
	}
	
	@Test
	public void testChooseSpreadSheetWithNullSpreadSheetName() {
		String result = mainController.chooseSpreadSheet(null);
		assertEquals(result,defaultSpreadSheetName);
	}
	
	@Test
	public void testInsertprojectsMetricsInfo() throws IOException, ServiceException{
		Date date = new Date();	
		String proyectKey1 = new String("proyectKey1");
		String proyectKey2 = new String("proyectKey2");
		String metricName1 = new String("metricName1");
		String metricName2 = new String("metricName2");
		String metricvalue1 = new String("metricValue1");
		String metricvalue2 = new String("metricValue2");
		
		List<ProjectMetricDTO> dtos = new ArrayList<ProjectMetricDTO>();
		dtos.add(new ProjectMetricDTO(date.toString(),proyectKey1, metricName1, metricvalue1));
		dtos.add(new ProjectMetricDTO(date.toString(),proyectKey2, metricName2, metricvalue2));
		
		SpreadsheetService service = mock(SpreadsheetService.class);
		URL listFeedUrl = new URL("http://www.google.com");
		
		when(service.insert((URL)anyObject(),(ListEntry)anyObject())).thenReturn(new ListEntry());
		
		mainController.insertprojectsMetricsInfo(dtos, service, listFeedUrl);
		
		Times times = new Times(2);
		verify(service,times).insert((URL)anyObject(),(ListEntry)anyObject());
		
	}

}
