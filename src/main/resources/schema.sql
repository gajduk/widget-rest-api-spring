CREATE TABLE IF NOT EXISTS widget (
    id bigint(10) NOT NULL AUTO_INCREMENT,
    xIndex INTEGER NOT NULL,
    yIndex INTEGER NOT NULL,
    zIndex INTEGER NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    updateTime TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX z_order
ON widget (zIndex);
CREATE INDEX x_order
ON widget (xIndex);