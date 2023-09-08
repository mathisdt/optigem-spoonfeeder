package org.zephyrsoft.optigemspoonfeeder.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940File;
import org.zephyrsoft.optigemspoonfeeder.mt940.parser.Mt940Parser;

@Service
public class ParseService {

	public Mt940File parse(InputStream inputStream) throws IOException {
		return Mt940Parser.parse(new LineNumberReader(new InputStreamReader(inputStream)));
	}

}
