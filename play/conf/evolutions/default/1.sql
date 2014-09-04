# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table route (
  id                        varchar(255) not null,
  source                    varchar(255),
  destination               varchar(255),
  timeout                   integer,
  cache                     integer,
  random_seed               double,
  constraint pk_route primary key (id))
;

create sequence route_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists route;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists route_seq;

