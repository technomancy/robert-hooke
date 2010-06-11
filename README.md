# Robert Hooke

Robert Hooke provides a flexible, composable mechanism by which you
can extend behaviour of functions after they've been defined. It's named
after [Robert Hooke FRS](http://en.wikipedia.org/wiki/Robert_Hooke), a
founding member of the Royal Society who made many important
discoveries in the fields of Gravitation, Microscopy, and Astronomy.

Add this to your project.clj :dependencies list:

    [robert/hooke "1.0.0"]

If you wish to make your project extensible using Robert Hooke, make
some clear conventions as to namespaces that will get loaded
automatically at startup so people wishing to extend your
functionality may place calls to add-hook there.

## Usage

    (use 'robert.hooke)

    (defn examine [x]
      (println x))

    (defn microscope
      "The keen powers of observation enabled by Robert Hooke allow
      for a closer look at any object!"
      [f x]
      (f (.toUpperCase x)))

    (defn doubler [f & args]
      (apply f args)
      (apply f args))

    (defn telescope [f x]
      (f (apply str (interpose " " x))))

    (add-hook #'examine microscope)
    (add-hook #'examine doubler)
    (add-hook #'examine telescope)

    (examine "something")
    > S O M E T H I N G
    > S O M E T H I N G

Hooks are functions that wrap other functions. They receive the
original function and its arguments as their arguments. Hook
functions can wrap the target functions in binding, change the
argument list, only run the target functions conditionally, or all
sorts of other stuff.

Technically the first argument to a hook function is not always the
target function; if there is more than one hook then the first hook
will receive a function that is a composition of the remaining
hooks. But when you're writing hooks, you should act as if it is the
target function.

If you would like to make your software extensible using Hooke, all
you need to do is provide a convention for namespaces that will get
loaded on startup. Then users can place files that call add-hook under
a specific namespace prefix (my.program.hooks.*) which they can rely
on getting loaded at startup.

## License

Copyright (C) 2010 Phil Hagelberg and Kevin Downey

Distributed under the Eclipse Public License, the same as Clojure.
