package com.googlecode.webutilities.test.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.googlecode.webutilities.util.Utils;
import com.mockrunner.mock.web.MockServletContext;
import com.mockrunner.mock.web.WebMockObjectFactory;

public class UtilsTest {
	
	 

	private static boolean start = true;
	
	private static File cssFile;
	private static String cssPath;
	
	
	@BeforeClass
	public static void init() throws IOException {
		cssFile = File.createTempFile("webutilities", ".css"); 
		FileWriter writer;
		writer = new FileWriter(cssFile);
		writer.write(".div { display:none;}");
		writer.close();
		cssPath = UtilsTest.class.getResource("/resources/css/a.css").getPath();
	}
	
	
	@Before
	public void createUpdater() {
		// create a single thread which updates the reference map with 'new images' every millisecond
		Executors.newSingleThreadExecutor().submit(new Runnable() {
			
			@Override
			public void run() {
				while(start) {
					try {
						// keep adding new empty img files to the css reference
						File imgFile = File.createTempFile("temp", ".img");
						Utils.updateReferenceMap(cssPath, imgFile.getAbsolutePath());
					} catch (IOException e1) {
						System.err.println(e1);
					}
					
					try {
						TimeUnit.MILLISECONDS.sleep(1);
					} catch (InterruptedException e) {
						return;
					}
				}				
			}
		
		});
		
	}
	
	@Test
	public void testConcurreny() {
		try {
			// setup
			WebMockObjectFactory factory = new WebMockObjectFactory();
			
			final List<String> resourcesToMerge = new ArrayList<String>();
			resourcesToMerge.add("css/a.css");
			
			MockServletContext context = factory.createMockServletContext();
			context.setRealPath("css/a.css", cssPath);
			
			for (int i=0;i<20;i++) {
				final String result  = Utils.buildETagForResources(resourcesToMerge, context);
				Assert.assertNotNull(result);
			}
		} catch (Exception e) {
			
			if (e instanceof ConcurrentModificationException ) {
				Assert.fail("ConcurrentModificationException happend, which should not happen");
			}
			e.printStackTrace();
			Assert.fail("Unexpected exception happend, which should not happen");
		} finally {
			start = false;
		}
	}
}
