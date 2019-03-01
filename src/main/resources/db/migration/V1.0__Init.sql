CREATE TABLE books(
  book_id VARCHAR(60) NOT NULL,
  book_title VARCHAR(100) NOT NULL,
  version INT NOT NULL,
  PRIMARY KEY(book_id, version)
)