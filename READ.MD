# Order Service
A springboot application provides RESTful endpoints for orders

# Deploy to Heroku

## Install Heroku CLI
npm install -g heroku

## Heroku Login
cd you-application
heroku login
heroku create 

#### Make a commit and push the changes to Heroku using git.
```
> git add . 
> git commit -m "initial commit"
> git push heroku master 
```
heroku has been added as one of the remotes already when you perform heroku create, you can verify that using git remote -v.

# DEBUGGING
let's see the logs of our application on Heroku to get some idea
``` 
> heroku logs --tail
```
