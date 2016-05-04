#	schema1.sql
#
#		Create the database used for our care demo application. This needs to
#	track users and ACL, as well as log all relevant transactions that take
#	place for audit purposes. HIPPA requires that we implement a reasonable
#	ACL system to limit access to only those items that are permissible for
#	a user to interact with, and requires we log all transactions for logging
#
#		We also assume a lower level logging implemented in our database, but
#	that is beyond the scope of this schema declaration. We also assume that
#	implementers will also promulgate appropriate policies, procedures and
#	training, again beyond the scope of this demo.
#
#		Note that the requirements of HIPPA are 

#-------------------------------------------------------------------------------#
#																				#
#	User Database																#
#																				#
#-------------------------------------------------------------------------------#

#	Users
#
#		User database. This is simply a table of users (by username), the
#	password (which is hashed using SHA256) and IDs. We also track just the
#	information necessary to reset the password; this is not a table which
#	contains patient information.
#
#	username: the username used to log in
#	password: the hashed password
#
#	email: the e-mail for password recovery
#	name: the user's name

CREATE TABLE Users (
	userid serial not null primary key,
	username text unique not null,
	
	email text unique not null,
	name text not null,
	
	password varchar(128) not null
);

CREATE INDEX UsersIX1 on Users ( username );
CREATE INDEX UsersIX2 on Users ( email );


#	UserAddress
#
#		User address information. This is an address associated with a user.
#	Note we assume a US formatted address.

CREATE TABLE UserAddress (
	addrid serial not null primary key,
	userid int not null,
	
	name text,
	addr1 text,
	addr2 text,
	city text,
	state text,
	postalcode text
);

#	UserPhone
#
#		User phone information. This is a phone number associated with the user

CREATE TABLE UserPhone (
	phoneid serial not null primary key,
	userid int not null,
	
	name text,
	phone text
);

#-------------------------------------------------------------------------------#
#																				#
#	Forgot password	tracking													#
#																				#
#-------------------------------------------------------------------------------#

CREATE TABLE ForgotPassword (
	recordid serial not null primary key,
	userid int not null,
	token varchar(256) not null unique,
	expires timestamp without time zone not null
);

CREATE INDEX ForgotPasswordIX1 on ForgotPassword ( token );

#
#	TODO: The patient database.
#

