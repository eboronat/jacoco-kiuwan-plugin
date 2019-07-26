package com.kiuwan.rules;
// MIT License
//
// Copyright (c) 2019 R. Meppelink, Kiuwan.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;

/**
 * @author rmepp
 * @author eboronat
 * This rule load 'Jacoco' report and generates kiuwan violations for each failed condition.
 */
public class JacocoKiuwanPlugin extends AbstractRule { 
	private final static Logger logger = Logger.getLogger(JacocoKiuwanPlugin.class);

	private String COBERTURA_REPORT_NAME;
	private double thresholdMax;
	private double thresholdMin;

	public void initialize (RuleContext ctx) { 
		super.initialize(ctx);	
		thresholdMax = getProperty("thresholdMax",50); //Take threshold from rule definition. Default 50
		thresholdMin = getProperty("thresholdMin",0); //Take threshold from rule definition. Default 0
		COBERTURA_REPORT_NAME = getProperty("COBERTURA_REPORT_NAME", "jacoco.xml"); //Take report name. Default jacoco.xml
		
		File baseDir = ctx.getBaseDirs().get(0);

		logger.debug("initialize: " +  this.getName() + " : " + baseDir);

	}


	protected void visit (BaseNode root, final RuleContext ctx) { 
		// this method is run once for all source files under analysis.
		// this method is left in blank intentionally.
	}


	public void postProcess (RuleContext ctx) { 
		// this method is run once for analysis
		super.postProcess(ctx); 
		logger.debug("postProcess: " +  this.getName());

		File baseDir = ctx.getBaseDirs().get(0);

		// iterates over 'cobertura' reports files.
		try {
			Files.walk(Paths.get(baseDir.getAbsolutePath()))
			.filter(Files::isRegularFile)
			.filter(p -> p.getFileName().toString().equals(COBERTURA_REPORT_NAME))
			.forEach(p -> {
				try {
					processCoberturaReportFile(ctx, p);
				} catch (ParserConfigurationException | SAXException | IOException e) {
					logger.error("Error parsing file " + p.getFileName() + ". ", e);
				}
			});
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	private void processCoberturaReportFile(RuleContext ctx, Path p) throws ParserConfigurationException, SAXException, IOException {
		logger.debug("processing: " +  p);

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		SAXParser parser = factory.newSAXParser();

		CoberturaReportHandler handler = new CoberturaReportHandler(ctx, p.toFile());
		parser.parse(p.toFile(), handler);
	}	

	/**
	 * The cobertura xml report handler
	 * @author rmepp
	 */
	class CoberturaReportHandler extends DefaultHandler {
		private RuleContext ctx;
		private File file;
		private Locator locator = null;

		private boolean inFoundClass = false;
		private boolean inMethod = false;
		private String fileName = "";

		public CoberturaReportHandler(RuleContext ctx, File file) {
			super();
			this.ctx = ctx;
			this.file = file;

			this.setDocumentLocator(locator);
		}

		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("class")) {					// Only process <class> where sourcefilename is found
				fileName = attributes.getValue("sourcefilename");
				logger.debug("CoberturaReportHandler.startElement(class" + ", "+ fileName + ")");
				try {
					Files.walk(Paths.get(ctx.getBaseDirs().get(0).getAbsolutePath()))
					.filter(Files::isRegularFile)
					.filter(pAnalyzed -> pAnalyzed.getFileName().toString().equals(fileName))
					.forEach(pAnalyzed -> {
						logger.debug("CoberturaReportHandler.startElement(Found file: " + fileName + ")");
						inFoundClass = true;
					});
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else if (inFoundClass && qName.equalsIgnoreCase("method")) {		// Skip contents of <method>
				logger.debug("CoberturaReportHandler.startElement(method)");
				inMethod = true;			
			} else if (inFoundClass && !inMethod && qName.equalsIgnoreCase("counter")) {
				String type  = attributes.getValue("type");
				if (type.equalsIgnoreCase("INSTRUCTION")) {
					String missed = attributes.getValue("missed");
					String covered = attributes.getValue("covered");
					double dMissed = Double.parseDouble(missed);
					double dCovered = Double.parseDouble(covered);
					double dCoverage = (dCovered / (dMissed + dCovered)) * 100.0;	//Calculate coverage
					DecimalFormat df = new DecimalFormat("#.##");
					df.setRoundingMode(RoundingMode.CEILING);
					
					if (thresholdMax > dCoverage && thresholdMin <= dCoverage) { 
						ctx.setSourceCodeFilename(new File(fileName));
						ctx.getReport().addRuleViolation(createRuleViolation(ctx, 1, "The coverage is: " + df.format(dCoverage) + "%",""));
					}
					logger.debug("CoberturaReportHandler.startElement(counter INSTRUCTION, "+ fileName + ", " + missed + ", " + covered + ", Coverage=" + df.format(dCoverage) + "%)");				
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equalsIgnoreCase("class")) {
				inFoundClass = false;
				logger.debug("CoberturaReportHandler.endElement(class)");
			} else if (inFoundClass && qName.equalsIgnoreCase("method")) {
				inMethod = false;
				logger.debug("CoberturaReportHandler.endElement(method)");
			}
		}
	}
}
