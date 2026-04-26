CREATE TABLE IF NOT EXISTS feature_flags (
    id UUID PRIMARY KEY,
    flag_key VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(280) NOT NULL,
    enabled BOOLEAN NOT NULL,
    environment_name VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS targeting_rules (
    feature_flag_id UUID NOT NULL,
    rule_order INTEGER NOT NULL,
    attribute_name VARCHAR(100) NOT NULL,
    operator_name VARCHAR(20) NOT NULL,
    rule_value VARCHAR(120) NOT NULL,
    target_version VARCHAR(20) NOT NULL,
    PRIMARY KEY (feature_flag_id, rule_order),
    CONSTRAINT fk_targeting_rules_feature_flag
        FOREIGN KEY (feature_flag_id)
        REFERENCES feature_flags (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_feature_flags_environment_name
    ON feature_flags (environment_name);

CREATE INDEX IF NOT EXISTS idx_targeting_rules_target_version
    ON targeting_rules (target_version);
