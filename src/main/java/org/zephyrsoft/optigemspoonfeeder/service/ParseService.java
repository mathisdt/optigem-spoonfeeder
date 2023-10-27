package org.zephyrsoft.optigemspoonfeeder.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.source.SourceFile;
import org.zephyrsoft.optigemspoonfeeder.source.parser.Mt940Parser;

@Service
public class ParseService {

	public SourceFile parse(InputStream inputStream) throws IOException {
		return Mt940Parser.parse(new LineNumberReader(new InputStreamReader(inputStream)));
	}

}
