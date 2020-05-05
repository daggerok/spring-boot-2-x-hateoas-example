-- drop sequence if exists users_sequence
-- ;
-- drop table "users" if exists
-- ;
-- drop schema "public" if exists
-- ;
create schema "public"
;
create sequence users_sequence start with 1 increment by 1
;
create table "users" (
    id varchar(36) not null,
    email varchar(255),
    full_name varchar(255),
    version bigint,
    primary key (id)
)
;
