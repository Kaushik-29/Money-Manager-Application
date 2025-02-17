CREATE DATABASE expense_tracker;

USE expense_tracker;

-- Table for storing user credentials
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Table for storing expenses
CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    date DATE,
    payee VARCHAR(50),
    description VARCHAR(255),
    amount DOUBLE,
    mode VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);


SELECT * FROM users;

SELECT * FROM expenses;
