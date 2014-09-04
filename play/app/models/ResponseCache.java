package models;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ResponseCache {
	public String body;
	public String contentType;
	public Set<Entry<String, List<String>>> headers;
	public int status;
}
