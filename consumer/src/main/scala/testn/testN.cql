CREATE KEYSPACE neopost WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE neopost.events (
	event_id timeuuid,
	object_parent_id timeuuid,
	event_originator text, //10char
	event_type text, //10char * 10
	event_code text, //10char * 100
	event_date_time timestamp,
	event_location text, //10char
	object_parent_status_code text,  //10char * 100
	notified boolean,
	event_meta_data text, //1000char
	event_details text, //50char
	deleted boolean,
	owner text, //10 char
    channel text, //10 char
    PRIMARY KEY ((event_id), owner, channel)
);


CREATE TABLE neopost.documents (
	document_id timeuuid,
	document_data_id text, //10char
	document_object_type text, //10char * 10
	document_primary_id text, //10char
	document_secondary_id text, //10char
	document_tertiary_id text, //10char
	document_quaternary_id text,//10char
	document text,//100000
	document_creation_date_time timestamp,
	document_update_date_time timestamp,
	document_type text, //10char
	document_template boolean,
	document_meta_data text, //1000char
	document_description text, //100char
	deleted boolean,
	PRIMARY KEY (document_id)
);






CREATE TABLE neopost.documents (
	document_id timeuuid,	document_data_id text,
	document_object_type text, 	document_primary_id text, 	document_secondary_id text, document_tertiary_id text,
	document_quaternary_id text, document text,	document_creation_date_time timestamp,	document_update_date_time timestamp,	document_type text,
	document_template boolean,	document_meta_data text, document_description text, deleted boolean,
	PRIMARY KEY (document_id)
);
