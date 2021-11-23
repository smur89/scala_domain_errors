# Domain Contexts
Sample app to demonstrate isolating domains

# Domains involved
- `Run` context of the App
  - This is the context in which the application is being run. In our case it's `IO`
  - The application is either running or it's failed and unrecoverable'
- `Handled` The context of our services 
  - This is the context used for expected errors
- `Traced` This context wraps the `Handled` context and provides a `ReaderT` with a `TracedContext` to access

# Alternatives to ReaderT
- Java’s TheadLocal possible for context propagation only in case of synchronous invocations;
- Monix has Local implementation, which can deal with context propagation for Task and Future.
- ZIO has FiberRef for context propagation in terms of fiber
- Cats Effect IO’s recently got Fiber Locals similar to previous solutions

# How to cause errors

## Service Errors
- Edit the `application.conf#external-api.uri` value to a different url (e.g. `external-api.uri = "https://google.com/"
  `) 

## Program Errors
- ```http POST localhost:3000/domain/short author=shane name=tony```

## Database Errors
- ```http POST localhost:3000/domain/long author=Shane name=Tony```

# Happy Path
  1.```http localhost:3000/domain```

  2.```http POST localhost:3000/domain/short author=Shane name=Tony```

  3.```http localhost:3000/domain```