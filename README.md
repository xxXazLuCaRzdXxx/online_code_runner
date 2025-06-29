# Online Code Runner Web App

Welcome to the Online Code Runner web application! This project allows you to execute code snippets in multiple programming languages directly from your browser, utilizing Docker container pooling for efficient code execution.

## Prerequisites
Before running the backend, ensure you have Docker Desktop installed and running on your machine. You can download it from Docker's official website.

## Step 0: Run the frontend (use the online-code-runner dir):

npm run start

## For the java backend:
## Step 1: Run the backend (use the java-code-runner dir):

java -jar target/java-code-runner-0.0.1-SNAPSHOT.jar


## OR


## For the javascript backend:
## Step 1: Pull docker images

Open your terminal or command prompt and pull the required Docker images by running the following commands:

docker pull python:3.9 

docker pull node:14 

docker pull ruby:2.7

## Step 2: Run the backend (use the online-code-backend dir):

node index.js




Open your web browser and go to http://localhost:5000 to access the Online Code Runner web application.
