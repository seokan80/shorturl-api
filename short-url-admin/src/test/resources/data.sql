-- Default Client Access Key for testing and development
-- Key Value: dev-test-key-12345
INSERT INTO TBL_CLIENT_ACCESS_KEY
    (NAME, KEY_VALUE, ISSUED_BY, DESCRIPTION, EXPIRES_AT, LAST_USED_AT, IS_ACTIVE, IS_DEL, DELETED_AT, CREATED_AT, UPDATED_AT)
VALUES
    ('Development Test Key',
     'dev-test-key-12345',
     'System',
     'Default client access key for development and testing purposes',
     NULL,
     NULL,
     'Y',
     'N',
     NULL,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP);

-- Default User for testing and development
-- Username: test-user
-- GroupName: test-group
INSERT INTO TBL_USER
    (USERNAME, GROUP_NAME, API_KEY, REFRESH_TOKEN, IS_DEL, DELETED_AT, CREATED_AT, UPDATED_AT)
VALUES
    ('test-user',
     'test-group',
     NULL,
     NULL,
     'N',
     NULL,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP);

-- Additional admin user for testing
INSERT INTO TBL_USER
    (USERNAME, GROUP_NAME, API_KEY, REFRESH_TOKEN, IS_DEL, DELETED_AT, CREATED_AT, UPDATED_AT)
VALUES
    ('admin-user',
     'admin-group',
     NULL,
     NULL,
     'N',
     NULL,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP);
