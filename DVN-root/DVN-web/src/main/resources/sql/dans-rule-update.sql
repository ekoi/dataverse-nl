-- Table: rule

-- DROP TABLE rule;

CREATE TABLE rule
(
  id serial NOT NULL,
  org_name character varying(255),
  description character varying(255),
  CONSTRAINT rule_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE rule
  OWNER TO "dvnApp";
-- Table: rule_condition

-- DROP TABLE rule_condition;

CREATE TABLE rule_condition
(
  id serial NOT NULL,
  attribute_name character varying(255),
  pattern character varying(255),
  rule_id bigint NOT NULL,
  CONSTRAINT rule_condition_pkey PRIMARY KEY (id),
  CONSTRAINT fk_rule_condition_rule_id FOREIGN KEY (rule_id)
      REFERENCES rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE rule_condition
  OWNER TO "dvnApp";
  
  
-- Table: rulegoal

-- DROP TABLE rulegoal;

CREATE TABLE rule_goal
(
  id serial NOT NULL,
  rule_id bigint NOT NULL,
  dvn_alias character varying(255),
  role_id bigint NOT NULL,
  CONSTRAINT rule_goal_pkey PRIMARY KEY (id),
  CONSTRAINT fk_rule_goal_rule_id FOREIGN KEY (rule_id)
      REFERENCES rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_rule_goal_role_id FOREIGN KEY (role_id)
      REFERENCES role (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE rule_goal
  OWNER TO "dvnApp";
