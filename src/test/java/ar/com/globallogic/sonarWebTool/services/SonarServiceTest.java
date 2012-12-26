package ar.com.globallogic.sonarWebTool.services;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.MetricQuery;
import org.sonar.wsclient.services.Query;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ar.com.globallogic.sonarWebTool.dtos.ProjectMetricDTO;
import ar.com.globallogic.sonarWebTool.exceptions.BusinessException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/application-contextTest.xml"})
@Configurable
public class SonarServiceTest extends TestCase{

	@Value("${sonar.defaultHost}")
	private String defaultUrl;
	
	@Value("${sonar.defaultuser}")
	private String defaultUser;
	
	@Value("${sonar.defaultPassword}")
	private String defaultPassword;
	
	@Value("${sonar.metric.default}")
	private String defaultMetric;
	
	@Autowired
	protected IMetricService sonarService;
	
	@Test
	public void testChooseSonarServerUrlWithNullInput(){
		SonarService sonarService = (SonarService) this.sonarService;
		assertEquals(sonarService.chooseSonarServerUrl(null),defaultUrl);
	}
	
	@Test
	public void testChooseSonarServerUrlWithValidInput(){
		SonarService sonarService = (SonarService) this.sonarService;
		String validUrl = "http://localhost:9000";
		assertEquals(sonarService.chooseSonarServerUrl(validUrl),validUrl);
	}
	
	@Test
	public void testchooseSonarUserWithNullInput(){
		SonarService sonarService = (SonarService) this.sonarService;
		assertEquals(sonarService.chooseSonarUser(null),defaultUser);
	}
	
	@Test
	public void testchooseSonarUserWithValidInput(){
		SonarService sonarService = (SonarService) this.sonarService;
		String user = "Sonar";
		assertEquals(sonarService.chooseSonarUser(user),user);
	}
	
	@Test
	public void testchooseSonarPasswordWithNullInput(){
		SonarService sonarService = (SonarService) this.sonarService;
		assertEquals(sonarService.chooseSonarPassword(null),defaultPassword);
	}
	
	@Test
	public void testchooseSonarPasswordWithValidInput(){
		SonarService sonarService = (SonarService) this.sonarService;
		String pass = "pass";
		assertEquals(sonarService.chooseSonarPassword(pass),pass);
	}
	
	@Test
	public void testchooseMetricToExecuteWithNullInput(){
		SonarService sonarService = (SonarService) this.sonarService;
		String[] metrics = new String[1];
		metrics[0] = defaultMetric;
		assertEquals(sonarService.chooseMetricToExecute()[0],metrics[0]);
	}
	
	@Test
	public void testchooseMetricToExecuteWithValidInput(){
		SonarService sonarService = (SonarService) this.sonarService;
		String metric = "coverage";
		String[] metrics = new String[1];
		metrics[0] = metric;
		assertEquals(sonarService.chooseMetricToExecute(metric)[0],metrics[0]);
	}
	
	@Test
	public void testValidateMetricsKeysWithValidMetricKeys(){
		SonarService sonarService = (SonarService) this.sonarService;
		Sonar sonar = mock(Sonar.class);
		
		String metricKey1 = "coverage";
		String metricKey2 = "lines";
		
		String[] metricsKeyToValidate = new String[2];
		metricsKeyToValidate[0] = metricKey1;
		metricsKeyToValidate[1] = metricKey2;
		
		List<Metric> metrics = new ArrayList<Metric>();
		
		//Mock metric list
		Metric metric1 = mock(Metric.class);
		Metric metric2 = mock(Metric.class);
		
		when(metric1.getKey()).thenReturn(metricKey1);
		when(metric2.getKey()).thenReturn(metricKey2);
		
		metrics.add(metric2);
		metrics.add(metric1);
		
		//Mock the method invocation
		when(sonar.findAll(Mockito.<MetricQuery>anyObject())).thenReturn(metrics);
		sonarService.validateMetricsKey(metricsKeyToValidate, sonar);
	}
	
	@Test
	@ExpectedException(BusinessException.class)
	public void testValidateMetricsKeysWithInvalidMetricKeys(){
		SonarService sonarService = (SonarService) this.sonarService;
		Sonar sonar = mock(Sonar.class);
		
		String metricKey1 = "coverage";
		String metricKey2 = "lines";
		String invalidMetricKey = "InvalidMetricKey";
		
		String[] metricsKeyToValidate = new String[2];
		metricsKeyToValidate[0] = metricKey1;
		metricsKeyToValidate[1] = invalidMetricKey;
		
		List<Metric> metrics = new ArrayList<Metric>();
		
		//Mock metric list
		Metric metric1 = mock(Metric.class);
		Metric metric2 = mock(Metric.class);
		
		when(metric1.getKey()).thenReturn(metricKey1);
		when(metric2.getKey()).thenReturn(metricKey2);
		
		metrics.add(metric2);
		metrics.add(metric1);
		
		//Mock the method invocation
		when(sonar.findAll(Mockito.<MetricQuery>anyObject())).thenReturn(metrics);
		sonarService.validateMetricsKey(metricsKeyToValidate, sonar);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testBuildResourcesWithNullProyectKey() {
		SonarService sonarService = (SonarService) this.sonarService;
		Sonar sonar = mock(Sonar.class);
		sonarService.buildResources(sonar, null,new String[2]);
		verify(sonar).findAll((Query<Resource>) anyObject());
	}
	
	@Test
	public void testBuildResourcesWithValidProyectKey() {
		SonarService sonarService = (SonarService) this.sonarService;
		
		String validProyectKey = "validProyectKey";
		
		Sonar sonar = mock(Sonar.class);
		when(sonar.find((ResourceQuery) anyObject())).thenReturn(new Resource());
		
		List<Resource> resources = sonarService.buildResources(sonar, validProyectKey,new String[2]);
		assertEquals(resources.size(),1);
	}
	
	@Test
	@ExpectedException(BusinessException.class)
	public void testBuildResourcesWithInvalidValidProyectKey() {
		SonarService sonarService = (SonarService) this.sonarService;
		
		String validProyectKey = "validProyectKey";
		
		Sonar sonar = mock(Sonar.class);
		when(sonar.find((ResourceQuery) anyObject())).thenReturn(null);
		
		sonarService.buildResources(sonar, validProyectKey,new String[2]);
	}
	
	@Test
	public void testbuildDtoList() {
		
		SonarService sonarService = (SonarService) this.sonarService;
		
		String metricKey1 = "metricKey1";
		String metricKey2 = "metricKey2";
		
		String proyectKey1 = "proyectKey1";
		String proyectKey2 = "proyectKey2";
		
		String metric1Value = "metric1Value";
		String metric2Value = "metric2Value";
		
		//Mock resources
		Resource resource1 = mock(Resource.class);
		when(resource1.getName()).thenReturn(proyectKey1);
		Resource resource2 = mock(Resource.class);
		when(resource2.getName()).thenReturn(proyectKey2);
		
		//Mock measures
		Measure measure1 = mock(Measure.class);
		when(measure1.getMetricKey()).thenReturn(metricKey1);
		when(measure1.getFormattedValue()).thenReturn(metric1Value);
		
		Measure measure2 = mock(Measure.class);
		when(measure2.getMetricKey()).thenReturn(metricKey2);
		when(measure2.getFormattedValue()).thenReturn(metric2Value);
		
		List<Measure> measures1 = new ArrayList<Measure>();
		measures1.add(measure1);
		
		List<Measure> measures2 = new ArrayList<Measure>();
		measures2.add(measure2);
		
		when(resource1.getMeasures()).thenReturn(measures1);
		when(resource2.getMeasures()).thenReturn(measures2);
		
		List<Resource> resources = new ArrayList<Resource>();
		resources.add(resource1);
		resources.add(resource2);
		
		List<ProjectMetricDTO> dtos = sonarService.buildDtoList(resources);
		
		ProjectMetricDTO dto = dtos.get(0);
		assertEquals(dto.getprojectName(),proyectKey1);
		assertEquals(dto.getMetricName(), metricKey1);
		assertEquals(dto.getMetricValue(),metric1Value);
		
		ProjectMetricDTO dto2 = dtos.get(1);
		assertEquals(dto2.getprojectName(),proyectKey2);
		assertEquals(dto2.getMetricName(), metricKey2);
		assertEquals(dto2.getMetricValue(),metric2Value);
	}
	
}
