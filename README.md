# Robert Hooke

Robert Hooke provides a flexible, composable mechanism by which you
can extend behaviour of functions after they've been defined. It's named
after [Robert Hooke FRS](http://en.wikipedia.org/wiki/Robert_Hooke), a
founding member of the Royal Society who made many important
discoveries in the fields of Gravitation, Microscopy, and Astronomy.

Add this to your project.clj :dependencies list:

    [robert/hooke "1.1.1"]

If you would like to make your software extensible using Hooke, all
you need to do is provide a convention for namespaces that will get
loaded on startup. Then users can place files that call add-hook under
a specific namespace prefix (my.program.hooks.*) which they can rely
on getting loaded at startup.

Hooks can change the behaviour of the functions they wrap in many
ways:

* binding
* conditional execution (may decide not to continue or decide to call
  a different function in some circumstances)
* modify arguments
* add side effects
* return different value

Hooke is inspired by Emacs Lisp's defadvice and clojure.test fixtures.

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
hooks. (Dare I say a continuation?) But when you're writing hooks, you
should act as if it is the target function.

Adding hooks to a defmulti is discouraged as it will make it
impossible to add further methods. Hooks are meant to extend functions
you don't control; if you own the target function there are obviously
better ways to change its behaviour.

## Bonus Features

Most of the time you'll never need more than just add-hook. But
there's more!

If you are using Hooke just to add side-effects to a function, it may
be simpler to use the append or prepend macros:

    (prepend print-name
      (print "The following person is awesome:"))

    (print-name "Gilbert K. Chesterton")
    > The following person is awesome:
    > Gilbert K. Chesterton

You may also run a block of code with the hooks for a given var
stripped out:

    (with-hooks-disabled print-name
      (print-name "Alan Moore"))
    > Alan Moore

## License

Copyright (C) 2010 Phil Hagelberg and Kevin Downey

Distributed under the Eclipse Public License, the same as Clojure.
