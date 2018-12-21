# Contribution Guidelines

Quality is important. Therefore we urge you to follow these guidelines if you want to contribute to this project.

## Version Control (Git)

### Give your commits a meaningful name

* If you are working on a submodule, name it at the beginning: "ixi: refuse ict if already connected to other ict".
* If you fixed a bug, use "fix: ...", like this: "rmi: fix: sending name of ixi instead of ict to ixi".
* "fix: spelling of the word 'contributions' in README.md" instead of ~~"fixed bug"~~.
* ~~"minor changes"~~ (which changes?).
* Keep commit names short and expressive, use the commit message for long texts instead.

### The Committing Process

* A commit should contain a self-contained change. So complete and commit a single task first before starting a new one - instead of mixing them together.
* Avoid excessive over-committing. Most commits should represent work that has been done within a 30-180 minute timeframe. Do not push 10 tiny commits with very few line changes. Instead, try to pack them into a bigger commit if possible. Depending on context, exceptions are possible here.
* Before committing, all unit tests must be run successfully and the code must be reformatted (see section ...).
* Where feasible, write a unit test which captures the essence of your changes.

## Code Style

We follow the "Clean Code" guidelines by Robert C. Martin:

### Naming
* Names are important. Keep them short and expressive.
* Avoid abbreviations. `catch(Throwable t) { ... }` is fine though.
* method names should include verbs: `validateSignature()`.
* Boolean attributes and simple queries should start with "is": `public boolean isValid()`

### Visibility and Data Encapsulation
* Data must be encapsulated where possible.
* Public attributes must be avoided. Exceptions: Data Objects (e.g. Transaction) and Builders (e.g. TransactionBuilders). Public attributes of Data Objects should be final.
* The inner workings of a class should be hidden as much as possible. Provide a public interface to operate on objects but keep the implementation secret.
* Visibility should be as private as possible. Only methods that are supposed to be called externally should be public.
* If you need to call a method from your jUnit tests, you are of course allowed to make them package-private.

### Method Complexity
* Keep parameters as low as possible.
* Methods should very rarely have more than 2 parameters. If you need more, consider building a separate class (e.g. Transfer.BalanceChangeCollector).
* Avoid spaghetti code! Methods should ideally have 3-7 lines of code. Use a hierarchical divide-and-conqueer strategy to separate them (see code example below).
* Avoid code duplication by moving duplicate code into a new method and calling it multiple times instead.
* Avoid nested blocks (if/else within loop within try/catch within loop). Try to have no more than one or two blocks within each method.
* Each method should only do one well-defined thing. Avoid `waterFlowersAndGoToWorK()`.

```java
// ===== Bad Code Example (Spaghetti Code) =====

public void makeSpaghetti() {
    buySpaghetti();
    buyCheese();
    buySouce();

    pot.fillWithWater();
    hotPlate.activate();
    while(!pot.isBoiling())
        try {
            Thread.sleep(20000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    pot.putInSpaghetti()
    try {
        Thread.sleep(300000);
    } catch(InterruptedException e) {
        e.printStackTrace();
    }
    while(!pot.isContentBoiled())
        try {
            Thread.sleep(60000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    // ...
}
```

```java
// ===== Good Code Example (Clean Code) =====

public void makeSpaghetti() {
    buyIngredients();
    boilSpaghetti();
    // ...
}

private void buyIngredients() {
    buyIngredients();
    buySpaghetti();
    buyCheese();
}

private void boilSpaghetti() {
    boilWater();
    pot.putInSpaghetti()
    sleep(30000);
    while(!pot.isContentBoiled())
        sleep(60000);
}

private void boilWater() {
    pot.fillWithWater();
    hotPlate.activate();
    while(!pot.isBoiling())
        sleep(20000);
}

private static void sleep(long ms) {
    try {
        Thread.sleep(ms);
    } catch(InterruptedException e) {
        e.printStackTrace();
    }
}
```

Notice how long you need to figure out how `makeSpaghetti()` works in the first example and how easy it is to spot what it does in the second example?
We also avoided duplication of `Thread.sleep()`. And the itendation level is lower because we don't have nested blocks (try/catch block within a loop).

### Class Complexity

* Avoid god classes. Keep classes slim (no more than 100-200 lines of code).
* Each class should have a well defined resposibility.
* Each class should be well separated from other classes and have few dependencies to others.
* A package should ideally contain 3-7 classes but no more than 10. Otherwise distribute your classes across sub-packages.
* Consider using nested classes where it makes sense to make things appear more simple from outside.

### Comments and Documentation

* Classes and public methods should have a JavaDoc comment.
    * use `@param`, `@return`, `@throws`
    * use `@link{}` instead of plain text names
    * optionally use `@see` if it helps
    * do not use `@version`, `@author`
* Use comments sparingly. Your code should be expressive enough to not require comments. Sometimes, a comment is necessary though (see the good code example where the comment prevents others from accidentally braking the code).
* Use `// TODO: ...` if something is not implemented yet. But make sure others understand what meant.

```java
// ===== Bad Code Example (Useless Comments) =====

/**
* This function makes spaghetti.
*/
public void makeSpaghetti() {
    // first buy ingredients
    buyIngredients();
    // then boil rice
    boilSpaghetti();
    // TODO: fix that bug that occured yesterday
}
```

```java
// ===== Good Code Example (Helpful Comment) =====

synchronized (queue) {
    // keep queue.isEmpty() within the synchronized block so notify is not called after the empty check and before queue.wait()
    queue.wait(queue.isEmpty() ? 0 : Math.max(1, queue.peek().sendingTime - System.currentTimeMillis()));
}
```

Notice the "rice" instead of "spaghetti"? This method was copied from `makeRice()`, the author just forgot to adjust the comment.
Someone might see this comment and change `boilSpaghetti()` into `boilRice()`, thus braking the method. Comments require
constant maintaining, otherwise they turn into lies. That's why we try to avoid them.