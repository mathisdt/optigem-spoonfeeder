package org.zephyrsoft.optigemspoonfeeder.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.service.ParseService;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import net.bzzt.swift.mt940.Mt940Entry;
import net.bzzt.swift.mt940.Mt940File;

@Route("")
class MainView extends VerticalLayout {

	private final OptigemSpoonfeederProperties properties;
	private final ParseService parseService;

	private TextArea log;

	MainView(OptigemSpoonfeederProperties properties, ParseService parseService) {
		this.properties = properties;
		this.parseService = parseService;

		log = new TextArea();
		log.setLabel("Log");
		log.setValueChangeMode(ValueChangeMode.EAGER);
		add(log);

		MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
		Upload upload = new Upload(buffer);

		upload.addSucceededListener(event -> {
			String fileName = event.getFileName();
			InputStream inputStream = buffer.getInputStream(fileName);
			parseUploadedFile(inputStream);
		});
		add(upload);
	}

	private void parseUploadedFile(InputStream inputStream) {
		try {
			Mt940File mt940 = parseService.parse(inputStream);
			List<Mt940Entry> mt940entries = mt940.getEntries();
			log("Parsed input file - " + mt940entries.size() + " entries found");

			// TODO apply rules

		} catch (IOException e) {
			log("Error: " + e.getMessage());
		}
	}

	private void log(String line) {
		log.setValue((StringUtils.isBlank(log.getValue()) ? "" : log.getValue() + "\n") + line);
	}
}