# redis-agent

I really find Redis convenient, but have run into some annoyances I'd
like to abstract away. I'm not trying to replace existing libraries,
such as carmine, but rather build on top of them.

Type is largely ignored when putting information in, and it creates
complications when I try to pull it back out. If I have a number
reflecting persistent state, every time I pull it out of redis I have
to use `read-string` to turn it back into the type I put in. So a
primary goal of this project is that any data that goes in looks the
same as when it comes out.

When I think about redis, it's more as a giant map than a key/value
store. I wish I could use the really nice facilities that exist to
interact with maps and other data structures, rather than be stuck
writing redis commands directly. Obviously there's a wrinkle in this
plan, though, which is most interactions are going to result in side
effects.

Side effects bring in the need for concurrency management, and we all
know Clojure has these in spades. Refs and atoms are inappropriate for
this type of issue, as they will both retry and write to Redis
multiple times. That leaves us with agents, so this library will allow
you to interact with redis as if it's a map agent.

## Usage

Once I get this up on clojars, I'll update how to include the library
here.

I don't want to write a quickstart that can get out of date with the
actual code, so I've [put it in a test
file](test/redis_agent/test_public_api.clj).

## License

Copyright © 2018 Colin Williams

Distributed under the GPLv3.
