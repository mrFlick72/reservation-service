create table reservation
(
  reservation_id  varchar(255) not null primary key,
  restaurant_name varchar(255),
  date            timestamp
);


create table customer
(
  reservation_id varchar(255),
  first_name     varchar(255),
  last_name      varchar(255)
);

