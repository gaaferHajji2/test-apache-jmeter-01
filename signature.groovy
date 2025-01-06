import java.net.URL;
import java.time.Instant;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;

URL url = sampler.getUrl();

String path = sampler.getPath();
String query = url.getQuery();

String timestamp = String.valueOf(Instant.now().toEpochMilli());

String timestampQuery = "timestamp=" + timestamp;

if (query != null && !query.isEmpty()) {
	query += "&" + timestampQuery;
} else {
	query = timestampQuery;
}

String httpMethod = sampler.getMethod();

String body = "";

if("POST".equals(httpMethod)) {
	StringBuilder postBody = new StringBuilder();

	for(JMeterProperty jmeterProperty: sampler.getArguments()){
	    HTTPArgument arg = (HTTPArgument) jmeterProperty.getObjectValue();
	
	    postBody.append(arg.getEncodedValue());
	}
	
	body = postBody.toString();
}


String secretKey = vars.get("secretKey");
String strToSign = secretKey;

strToSign += Stream.of(query.split("&")).map(keyValue -> keyValue.split("=")[0] + keyValue.split("=")[1]).sorted().collect(Collectors.joining(""));
strToSign += body;
strToSign += secretKey;

log.info("The String To Sign Is: {}", strToSign);
log.info("The Body Of The Request Is: {}", body);

log.info("The Path Is: {}", path);

String sign = DigestUtils.md5Hex(strToSign.getBytes("UTF-8"));

String signQuery = "sign=" + sign;

if(path.startsWith("/city")){
	
	if (path.contains("?")) {
		sampler.setPath(path + "&" + timestampQuery + "&" + signQuery);
	} else {
		sampler.setPath(path + "?" + timestampQuery + "&" + signQuery);
	}
} else {
	HeaderManager headerManager = sampler.getHeaderManager();

	if(headerManager == null){
		headerManager = new HeaderManager();
		sampler.setHeaderManager(headerManager);
	}

	headerManager.removeHeaderNamed("sign");
	
	
	headerManager.add(new Header("sign", sign));

	if (path.contains("?")) {
		sampler.setPath(path + "&" + timestampQuery);
	} else {
		sampler.setPath(path + "?" + timestampQuery);
	}
}

