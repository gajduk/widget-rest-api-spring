CREATE TABLE IF NOT EXISTS widget (
    id UUID NOT NULL,
    xIndex INTEGER NOT NULL,
    yIndex INTEGER NOT NULL,
    zIndex INTEGER NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    updateTime TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX x_order
ON widget (xIndex);
CREATE UNIQUE INDEX z_order
ON widget (zIndex);