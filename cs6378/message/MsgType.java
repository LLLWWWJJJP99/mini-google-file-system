package cs6378.message;

public interface MsgType {
	String LOGIN = "login";
	String LOGIN_SUCCESS = "login_success";
	String REPLY = "reply";
	String REQUEST = "request";
	String HEARTBEAT = "heartbeat";
	String CREATE = "create";
	String APPEND = "append";
	String READ = "read";
	String FAILURE = "failure";
	String CHUNK_DELIMITER = "chunk_delimiter";
	// two phase commit protocol
	String GET_ALIVE_SERVERS = "get_alive_servers";
	String COMMIT_REQ = "commit_req";
	String COMMIT = "commit";
	String ABORT = "abort";
	String AGREE = "agree";
	
	// wakeup message
	String QUERY_VERSION = "query_version";
	String GET_CONTENT = "get_content";
	String SEND_CONTENT = "send_content";
			
}
