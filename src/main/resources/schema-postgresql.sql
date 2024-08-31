--CREATE TABLE FUN_STUFF(ID NUMERIC NOT NULL PRIMARY KEY, thing1 VARCHAR, thing2 VARCHAR);
--
--INSERT INTO FUN_STUFF(ID, thing1, thing2) VALUES (1, 'rt1', 'rt3');
--INSERT INTO FUN_STUFF(ID, thing1, thing2) VALUES (2, 'rt1111', 'rt3341254');

CREATE TABLE oauth2_authorized_client (
  client_registration_id varchar(100) NOT NULL,
  principal_name varchar(200) NOT NULL,
  access_token_type varchar(100) NOT NULL,
  access_token_value bytea NOT NULL,
  access_token_issued_at timestamp NOT NULL,
  access_token_expires_at timestamp NOT NULL,
  access_token_scopes varchar(1000) DEFAULT NULL,
  refresh_token_value bytea DEFAULT NULL,
  refresh_token_issued_at timestamp DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (client_registration_id, principal_name)
);