package com.nh.shorturl.type;

public enum ApiResult {
	SUCCESS("0000", "Success"),
	FAIL("9999", "Fail"),

	// BAD REQUEST
	INVALID_PARAMETER("1001", "Invalid Parameter"),

	NOT_FOUND("1404", "Resource Not Found"),
	BOT_NOT_FOUND("1404", "Bot resource not found"),
	FAQ_NOT_FOUND("1404", "requested faq resource(s) not found"), // 2024.02.27 추가
	USER_NOT_FOUND("1404", "User resource not found"),
	NAMED_ENTITY_NOT_FOUND("1404", "NamedEntity not found"),
	NAMED_ENTITY_VALUE_TAG_NOT_FOUND("1404", "NamedEntityValueTag not found"),
	WORKSPACE_NOT_FOUND("1404", "Workspace resource not found"),
	INTENT_CLASSIFICATION_ENTITY_NOT_FOUND("1404", "IntentClassificationEntity not found"),
	INTENT_CLASSIFICATION_ENTITY_VALUE_TAG_NOT_FOUND("1404", "IntentClassificationEntityValueTag not found"),
	LEARN_HISTORY_NOT_FOUND("1404", "Learn History not found"),
	LLM_PROMPT_TEMPLATE_NOT_FOUND("1404", "LLM Prompt Template not found"),


	BOT_SCRIPT_IN_USE("2000", "BOT Script in use"),
	BOT_SCRIPT_IN_USE_INTENT("2001", "BOT Script in use"),
	BOT_SCRIPT_NOT_FOUND("2004", "BOT Script not found"),

	NAMED_ENTITY_IN_USE("1000", "NamedEntity in use"),
	NAMED_ENTITY_NOT_EXISTS("1404", "NamedEntity not exists"),
	INTENT_IN_USE("1000", "Intent in use"),
	INTENT_CLASSIFICATION_ENTITY_IN_USE("1000", "IntentClassificationEntity in use"),

	// >>> 봇, 워크스페이스 학습/배포대기/배포
	WORKSPACE_CANNOT_LEARN_STATUS("1002", "Cannot learn workspace"),
	WORKSPACE_CANNOT_LEARN_STATUS_LEARNING("1003",
		"Cannot learn workspace - workspace is learning, verifying or deploying"),

	WORKSPACE_CANNOT_PREDEPLOY_STATUS("1002", "Cannot predeploy workspace"),
	WORKSPACE_CANNOT_PREDEPLOY_STATUS_DEPLOYING("1003",
		"Cannot predeploy workspace - workspace is learning, verifying or deploying"),
	WORKSPACE_CANNOT_PREDEPLOY_STATUS_INVALID_VERSION("1004", "Cannot predeploy workspace - invalid workspace version"),

	WORKSPACE_CANNOT_DEPLOY_STATUS("1002", "Cannot deploy workspace"),
	WORKSPACE_CANNOT_DEPLOY_STATUS_DEPLOYING("1003",
		"Cannot deploy workspace - workspace is learning, verifying or deploying"),
	WORKSPACE_CANNOT_DEPLOY_STATUS_INVALID_VERSION("1004", "Cannot deploy workspace - invalid workspace version"),
	BOT_CANNOT_LEARN_STATUS("1002", "Cannot learn bot"),
	BOT_CANNOT_LEARN_STATUS_LEARNING("1003", "Cannot learn bot - bot is learning, verifying or deploying"),
	WORKSPACE_LEARN_HISTORY_NOT_EXISTS("1404", "Workspace learn history not found"),
	WORKSPACE_PREDEPLOY_HISTORY_NOT_EXISTS("1404", "Workspace predeploy history not found"),
	WORKSPACE_CONFIGURATION_ERROR("9999", "Invalid workspace configuration - check configuration values"),

	BOT_CANNOT_PREDEPLOY_STATUS("1002", "Cannot predeploy bot"),
	BOT_CANNOT_PREDEPLOY_STATUS_LEARNING("1003", "Cannot predeploy bot - bot is learning, verifying or deploying"),

	BOT_CANNOT_DEPLOY_STATUS("1002", "Cannot deploy bot"),
	BOT_CANNOT_DEPLOY_STATUS_LEARNING("1003", "Cannot deploy bot - bot is learning, verifying or deploying"),
	BOT_CANNOT_DEPLOY_STATUS_INVALID_VERSION("1002", "Cannot deploy bot - Production Version is already latest."),
	BOT_CANNOT_DEPLOY_STATUS_ZERO_VERSION("1002", "Cannot deploy bot - Staging Version is 0."),

	BOT_CANNOT_CANCEL_STATUS("1003", "Cannot cancel learning bot - bot already finished learning"),
	WORKSPACE_CANNOT_CANCEL_STATUS("1003",
		"Cannot cancel learning or deploying workspace - workspace is not learning or deploying"),
	WORKSPACE_ALREADY_COMPLETE("1003", "워크스페이스가 이미 학습/배포 완료 되었습니다."),
	WORKSPACE_ALREADY_FAIL("1003", "워크스페이스가 학습/배포 실패 하였습니다."),

	WORKSPACE_CANNOT_UPDATE_STATUS_LEARNING("1003", "Cannot update workspace - workspace is learning or deploying"),
	// <<< 학습

	NAME_TOO_LONG("9999", "이름은 255자를 넘길 수 없습니다."),
	DESCRIPTION_TOO_LONG("9999", "설명은 255자를 넘길 수 없습니다."),
	DUPLICATE_CODE("9999", "중복된 코드가 존재합니다."),
	CODE_TOO_LONG("9999", "코드는 255자를 넘길 수 없습니다."),
	CODE_EMPTY("9999", "코드를 입력한 뒤 등록해주세요."),
	RELATED_SLOT_EXISTS("9999", "해당 템플릿을 사용하는 슬롯노드/답변노드가 존재합니다."),
	RELATED_TEMPLATE_EXISTS("9999", "해당 컴포넌트를 사용하는 템플릿이 존재합니다."),
	RELATED_SETTING_TEMPLATE_EXISTS("9999", "멀티모달 설정에서 해당 템플릿을 사용 중입니다."),
	CHECK_FILE_TYPE("9999", "파일 확장자를 확인해주세요."),
	CHECK_FILE_DATA("9999", "파일 내용을 확인해주세요."),

	//MULTIMODAL SERVER
	INVALID_TEMPLATE("9999", "해당 요청에 대한 캐시가 존재하지 않습니다"),
	// Internal Server Error
	INTERNAL_ERROR("500", "내부 연동 에러"),

	// 연동 오류
	EXTERNAL_CONNECTION_ERROR("2000", "외부 서버와의 연결에 실패하였습니다."),
	ENGINE_SESSION_ERROR("2001", "상담 세션이 만료되었거나 시나리오 진행을 할 수 없습니다."),
	ENGINE_SYSTEM_ERROR("2002", "엔진 내부에서 에러가 발생했습니다."),
	ENGINE_TIMEOUT("2003", "봇 응답 시간이 초과되었습니다. 다시 말씀해주세요."),
	EXTERNAL_INVALID_RESPONSE_ERROR("2004", "외부 서버에서 비정상 응답을 리턴했습니다."),

	CAN_NOT_UPDATE_CUSTOM_COMPONENT_ON_SERVICE("9999", "컴포넌트가 템플릿에 사용 중이기 때문에 서비스 여부 체크를 해지할 수 없습니다."),

	CAN_NOT_UPDATE_MULTI_MODAL_TEMPLATE_ON_SERVICE("9999", "템플릿이 슬롯 노드/답변 노드에 사용 중이기 때문에 서비스 여부 체크를 해지할 수 없습니다."),

	MULTI_MODAL_TEMPLATE_NOT_FOUND("9999", "선택하신 멀티모달 템플릿을 찾을 수 없습니다."),

	// 인증 오류
	UNAUTHORIZED("1401", "인증이 필요합니다."),
	FORBIDDEN("1403", "권한이 없습니다");

	public static final String CODE_NOT_FOUND = "1404";
	public static final String CODE_INVALID = "9001";
	public static final String CODE_BAD_REQUEST = "1001";

	private final String code;
	private final String message;

	ApiResult(String code, String message) {
		this.code = code;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public static ApiResult findByCode(String code) {
		for (ApiResult apiResult : ApiResult.values()) {
			if (apiResult.getCode().equals(code)) {
				return apiResult;
			}
		}
		return null;
	}
}
