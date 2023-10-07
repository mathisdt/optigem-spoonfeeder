package org.zephyrsoft.optigemspoonfeeder.service;

import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.Konto;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940Entry;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940Entry.SollHabenKennung;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940File;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940Record;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HibiscusImportService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private final OptigemSpoonfeederProperties properties;
	private XmlRpcClient client;

	public HibiscusImportService(OptigemSpoonfeederProperties properties) {
		this.properties = properties;
		if (isConfigured()) {
			client = createXmlRpcClient();
		}
	}

	public boolean isConfigured() {
		boolean result = Objects.nonNull(properties.getHibiscusServerUrl())
				&& StringUtils.isNotBlank(properties.getHibiscusServerUsername())
				&& StringUtils.isNotBlank(properties.getHibiscusServerPassword());
		log.info("Hibiscus is configured: {}", result);
		return result;
	}

	public boolean isReachable() {
		try {
			client.execute("hibiscus.xmlrpc.konto.find", Collections.emptyList());
			log.info("Hibiscus is reachable: true");
			return true;
		} catch (Exception e) {
			log.info("Hibiscus is reachable: false ({})", e.getMessage());
			return false;
		}
	}

	public boolean isConfiguredAndReachable() {
		return isConfigured() && isReachable();
	}

	public List<Konto> getKonten() {
		if (isConfigured()) {
			Object result = null;
			try {
				result = client.execute("hibiscus.xmlrpc.konto.find", Collections.emptyList());
			} catch (XmlRpcException e) {
				throw new IllegalStateException("could not fetch accounts", e);
			}

			List<Konto> ret = new ArrayList<>();
			if (result != null) {
				for (Object object : (Object[]) result) {
					@SuppressWarnings("unchecked")
					Map<String, String> fetched = (Map<String, String>) object;
					String name = fetched.get("name");
					String iban = fetched.get("iban");
					String tableAccounts = null;
					String tableProjects = null;
					if (properties.getHibiscusServerAccount() != null
							&& properties.getHibiscusServerAccount().containsKey(iban)) {
						OptigemSpoonfeederProperties.AccountProperties accountProperties = properties.getHibiscusServerAccount().get(iban);
						name = accountProperties.getName();
						tableAccounts = accountProperties.getTableAccounts();
						tableProjects = accountProperties.getTableProjects();
					}
					Konto account = new Konto(name, iban, fetched.get("id"), tableAccounts, tableProjects);
					ret.add(account);
				}
			}
			Collections.sort(ret);

			return ret;
		} else {
			return Collections.emptyList();
		}
	}

	public Mt940File read(YearMonth month, Konto konto) {
		List<Mt940Entry> entries = new ArrayList<>();

		Map<String, String> params = new HashMap<>();
		params.put("datum:min", DATE.format(month.atDay(1)));
		params.put("datum:max", DATE.format(month.atEndOfMonth()));
		params.put("konto_id", konto.getId());

		Object result = null;
		try {
			result = client.execute("hibiscus.xmlrpc.umsatz.list", new Object[] { params });
		} catch (XmlRpcException e) {
			throw new IllegalStateException(e);
		}

		Object[] array = (Object[]) result;
		if (array != null) {
			for (Object object : array) {
				@SuppressWarnings("unchecked")
				Map<String, String> fetched = (Map<String, String>) object;
				Mt940Entry posting = new Mt940Entry();
				posting.setKontobezeichnung(konto.getIban());
				posting.setBuchungstext(fetched.get("art"));
				posting.setValutaDatum(LocalDate.parse(fetched.get("valuta"), DateTimeFormatter.ISO_LOCAL_DATE));
				posting.setName(fetched.get("empfaenger_name"));
				posting.setVerwendungszweck(fetched.get("zweck"));
				posting.setKontoNummer(fetched.get("empfaenger_konto"));
				posting.setBankKennung(fetched.get("empfaenger_blz"));
				BigDecimal amount = parseBigDecimal(fetched.get("betrag"));
				posting.setSollHabenKennung(amount.compareTo(BigDecimal.ZERO) > 0
						? SollHabenKennung.CREDIT
						: SollHabenKennung.DEBIT);
				posting.setBetrag(posting.getSollHabenKennung() == SollHabenKennung.CREDIT
						? amount
						: amount.negate());

				entries.add(posting);
			}
		}
		Collections.reverse(entries);
		return Mt940File.builder()
				.records(List.of(Mt940Record.builder()
						.entries(entries)
						.build()))
				.build();
	}

	private static BigDecimal parseBigDecimal(String in) {
		if (in.contains(",")) {
			// take care of the German number formatting of Hibiscus
			in = in.replaceAll("\\.", "").replaceAll(",", ".");
		}
		return new BigDecimal(in);
	}

	private XmlRpcClient createXmlRpcClient() {
		// create client configuration
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(properties.getHibiscusServerUrl());
		config.setBasicUserName(properties.getHibiscusServerUsername());
		config.setBasicPassword(properties.getHibiscusServerPassword());

		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);

		if (properties.getHibiscusServerUrl().getProtocol().equalsIgnoreCase("https")) {
			// ignore certificate validation errors (Hibiscus uses a self-signed cert)
			disableCertCheck();
		}

		return client;
	}

	private static void disableCertCheck() {
		TrustManager dummy = new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				// nothing to do
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				// nothing to do
			}
		};

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
			try {
				sc.init(null, new TrustManager[] { dummy }, new SecureRandom());
			} catch (KeyManagementException e) {
				throw new IllegalStateException("key error", e);
			}
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("algorithm not found", e);
		}
		HostnameVerifier dummy2 = new HostnameVerifier() {
			@Override
			public boolean verify(String host, SSLSession session) {
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(dummy2);
	}
}
