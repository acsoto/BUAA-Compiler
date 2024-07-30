24.7.30 前来更新一下这个README。发现有很多学弟学妹有参考到这个仓库，甚至前两年有学弟好几次来很详细地问了代码里面的很多实现和思路，本以为大家不屑于写PCODE，没想到能帮助到大家，还是很感恩得到认可，有问到好多我自己也想不起来的于是又重新看了代码和文档，也是帮我重新完整的复习了一遍。

之前里面的英语一大堆语法错误，所以大致修改了一下。另外那个有点迷惑的Before Coding After Coding是因为课程要求文档要写代码前后的设计思路，保留了这个写法。

---

可切换分支查看每个设计阶段的代码，分为词法分析，语法分析，错误处理，代码生成四个部分。

设计有限自动机进行词法分析，处理非法字符，建立抽象语法树，使用递归下降方法进行语法分析和错误处理，将SysY语言编译为PCode代码，最终设计了对应的虚拟机进行解释执行。


## Lexical Analysis 词法分析

### Before Coding

Requirement: Read `testfile.txt`, parse every character into words and print them. At the same time, memorize the type, content, and line number of each word.

#### File reading

Read by line, scan every character of every string and analyze.

```java
while ((s = bf.readLine()) != null) {
	...
}
```

#### Analyse

Upon identifying the keyword, proceed to the next analysis.

```java
while ((c = getChar()) != null) {
  if (c == ' ' || c == '\r' || c == '\t') {
    continue;
  } else if (c == '+' || c == '-' || c == '*' || c == '%') {
    words.add(new Word(c));
  } else if (c == '/') {
    analyseSlash();
  } else if (c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}') {
    words.add(new Word(c));
  } else if (c == '>' || c == '<' || c == '=' || c == '!') {
    analyseRelation(c);
  } else if (c == ',' || c == ';') {
    words.add(new Word(c));
  } else if (c == '"') {
    analyseCitation();
  } else if (c == '&' || c == '|') {
    analyseLogic(c);
  } else if (Character.isDigit(c)) {
    analyseDigit(c);
  } else if (Character.isLetter(c) || c == '_') {
    analyseLetter(c);
  }
}
```

##### Common

For example, when encountering '+', directly create a new Word and typify it as "PLUS".

##### Function

For example

When encountering `<`, enter the function `analyseRelation` to read one more character. If it is `=`, analyze as `LEQ`...

```java
if (c == '<') {
  c = getChar();
  if (c == '=') {
    words.add(new Word("<="));
  } else {
    unGetChar();
    words.add(new Word("<"));
  }
```

`analyseLogic` is as the same.

##### Digit and Letter

Digit: When encountering a digit, scan a series of digits and turn them into a Word typified as "INTCON".

Letter: When encountering a letter, scan a string of letters or digits. It may be an "IDENFR" or "STRCON", depending on whether it is in the key map or not.

#### Word

class Word:

```java
public class Word {
    private String identification;
    private String content;
    private String type;
}
```

Encapsulate the initial function so that only `new Word(...)` is needed in the main processor, which will create the corresponding word.

For example

```java
    public Word(char identification) {
        this.identification = String.valueOf(identification);
        this.type = new KeyWordMap().getType(this.identification);
        this.content = this.identification;
    }
```

As for KeyWordMap, it is a HashMap that maps the string of a word to its type.

```java
    public KeyWordMap() {
        keyWords = new HashMap<>();
        keyWords.put("main", "MAINTK");
        keyWords.put("const", "CONSTTK");
        keyWords.put("int", "INTTK");
        ...
        }
```

### After Coding

#### File reading


Reading the file line by line is not convenient for prereading and undoing, so the file is read into a single String at first.

The method involves reading the file line by line, adding `\n` after every line, and scanning every character. When `\n` is encountered, `lineNum++`

```java
    private String transferFileToCode() {
        BufferedReader bf = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer();
        String s = null;
        while ((s = bf.readLine()) != null) {
            buffer.append(s).append("\n");
        }
        return buffer.toString();
    }
```

#### Analyse

Regarding analysis, it is different from what was described before coding.

First, words need to be analyzed one by one, so a global variable `index` is added to remember the pointer's position.

Additionally, situations may arise where reading one more character or undoing a read is necessary, so the functions `ungetChar` and `getChar` are encapsulated to facilitate the analysis.

```java
    private Character getChar() {
        if (index < code.length()) {
            char c = code.charAt(index);
            if (c == '\n') {
                lineNum++;
            }
            index++;
            return c;
        } else {
            return null;
        }
    }

    private void unGetChar() {
        index--;
        char c = code.charAt(index);
        if (c == '\n') {
            lineNum--;
        }
    }
```

##### Slash

1) `//` : When it comes to `\n` , stop.

```java
do {
  c = getChar();
  if (c == null || c == '\n') {
    return;
    // 判断为//注释，结束分析
  }
} while (true);
```

2) `/* */`: Get char until `*/` appears.

```java
do {
  c = getChar();
  if (c == null) {
    return;
  }
  if (c == '*') {
    c = getChar();
    if (c == '/') {
      return;
      // 判断为/* */注释，直接结束分析
    } else {
      unGetChar();
    }
  }
} while (true);
```

## Grammar Analysis 语法分析 

Requirement: Based on the words identified by the lexical analysis program, identify various grammatical elements according to the grammatical rules. The recursive descent method is used to analyze the grammatical components defined in the grammar.

### Before Coding

#### Data Reading

Similar to the lexical analysis, functions like `getWord` and `getNextWord` are prepared. Additionally, there is a global variable `(Word) curWord` to indicate the current word when reading `ArrayList<Word> words` from the lexical analysis one by one.

The analysis strategy is as follows:

- For normal rules: Keep getting words and analyze them.
- For expression rules: First, scan the entire expression using the function `getExp`. Then, divide the expression and use the recursive descent method to analyze it.


`getExp` like

```java
    private ArrayList<Word> getExp() {
        ArrayList<Word> exp = new ArrayList<>();
        while (true) {
            if (word is symbol of end) {
                break;
            }
            ...
            getWordWithoutAddToGrammar();
            exp.add(curWord);
            word = getNextWord();
        }
        return exp;
    }
```

#### recursive descent

According to grammatical rules, code functions for each term of the rule.

Main idea: Read a word, check what it symbolizes, and enter the next analyzing function.

For example:

to

```c
CompUnit → {Decl} {FuncDef} MainFuncDef // 1.是否存在Decl 2.是否存在 FuncDef
```

I analyze like this:

```java
private void analyseCompUnit() {
  Word word = getNextWord();
  while (word.typeEquals("CONSTTK") || (
    word.typeEquals("INTTK") && getNext2Word().typeEquals("IDENFR") && !getNext3Word().typeEquals("LPARENT"))) {
    analyseDecl();
    word = getNextWord();
  }
  while (word.typeEquals("VOIDTK") || (
    (word.typeEquals("INTTK") && !getNext2Word().typeEquals("MAINTK")))) {
    analyseFuncDef();
    word = getNextWord();
  }
  if (word.typeEquals("INTTK") && getNext2Word().typeEquals("MAINTK")) {
    analyseMainFuncDef();
  } else {
    error();
  }
  grammar.add("<CompUnit>");
}
```

`grammar` is used to memorize the output of both the lexical analysis and the grammar analysis lists.

#### left recursion

```java
加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp // 1.MulExp 2.+ 需覆盖 3.- 需覆盖
```

Check if the expression contains '+' or '-'. If it does, separate the expression into `AddExp` and `MulExp`. Then analyze them separately.

### After Code

#### left recursion

The previous method is not perfect for recursive descent. Therefore, the approach has been revised and rewritten.

to

```c
加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp // 1.MulExp 2.+ 需覆盖 3.- 需覆盖
```

Rewrite it like 

```c
AddExp → MulExp ('+' | '−') MulExp  ('+' | '−') MulExp ...
```

Code like 

```java
private void analyseMulExp(ArrayList<Word> exp) {
  Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("MULT", "DIV", "MOD")));
  int j = 0;
  for (ArrayList<Word> exp1 : exps.getWords()) {
    analyseUnaryExp(exp1);
    grammar.add("<MulExp>");
    if (j < exps.getSymbols().size()) {
      grammar.add(exps.getSymbols().get(j++).toString());
    }
  }
}
```


Function `divideExp` is used to divide the entire expression passed by `getExp` or a preceding function.

`divideExp`:

**Input:** 
- Original expression: `exp`
- Stop symbol: `symbol`

**Output:**
- List of divided expressions and symbols.

```java
private Exps divideExp(ArrayList<Word> exp, ArrayList<String> symbol) {
  ArrayList<ArrayList<Word>> exps = new ArrayList<>();
  ArrayList<Word> exp1 = new ArrayList<>();
  ArrayList<Word> symbols = new ArrayList<>();
  boolean unaryFlag = false;
  int flag1 = 0;
  int flag2 = 0;
  for (int i = 0; i < exp.size(); i++) {
    Word word = exp.get(i);
    if (word.typeEquals("LPARENT")) {
      flag1++;
    }
    if (word.typeEquals("RPARENT")) {
      flag1--;
    }
    if (word.typeEquals("LBRACK")) {
      flag2++;
    }
    if (word.typeEquals("RBRACK")) {
      flag2--;
    }
    if (symbol.contains(word.getType()) && flag1 == 0 && flag2 == 0) {
      //UnaryOp
      if (word.typeOfUnary()) {
        if (!unaryFlag) {
          exp1.add(word);
          continue;
        }
      }
      exps.add(exp1);
      symbols.add(word);
      exp1 = new ArrayList<>();
    } else {
      exp1.add(word);
    }
    unaryFlag = word.typeEquals("IDENFR") || word.typeEquals("RPARENT") || word.typeEquals("INTCON") || word.typeEquals("RBRACK");
  }
  exps.add(exp1);
  return new Exps(exps, symbols);
}
```

`Exps`

```java
public class Exps {
    private ArrayList<ArrayList<Word>> words;
    private ArrayList<Word> symbols;
}
```

#### other bugs

Most bugs are produced by the functions `getExp` and `divideExp` due to some overlooked situations, often resulting in errors like index out of range. Therefore, adjustments were made to some symbols for stopping the expression parsing and modifications were made to the rules for dividing or not dividing the expression, among other changes.

## Error Handling 错误处理

### Before Coding

#### Create the symbol table

Symbol class

```java
public class Symbol {
    private String type;
    private int intType;
    private String content;
    private int area = 0;
}
```

Type represents the type of the symbol.

- `IntType` is an integer. If it's 0, the symbol is an int. If it's 1, the symbol is an int[]. If it's 2, the symbol is an int[][], and so on.

`Content` is its content.

`Area` indicates where it is.

A `HashMap` of Symbols is created to memorize symbols created in each area.

When entering a new area, `area++`. When leaving an area, `area--`, with the corresponding symbols being destroyed.

```java
    private HashMap<Integer, Symbols> symbols = new HashMap<>();
    private HashMap<String, Function> functions = new HashMap<>();
    private ArrayList<Error> errors = new ArrayList<>();
    private int area = -1;
    private boolean needReturn = false;
    private int whileFlag = 0;
```

`needReturn` indicates whether the current function needs to return.

`whileFlag` indicates whether the current code block is within a while loop.

#### Errors

##### **a**

Just check format.

```java
public boolean isFormatIllegal() {
  for (int i = 1; i < content.length() - 1; i++) {
    char c = content.charAt(i);
    if (!isLegal(c)) {
      if (c == '%' && content.charAt(i + 1) == 'd') {
        continue;
      }
      return true;
    } else {
      if (c == '\\' && content.charAt(i + 1) != 'n') {
        return true;
      }
    }
  }
  return false;
}
```

##### **b c**

B: Every time an identifier is encountered, check if the same symbol has already been defined in the current area.

```java
    private boolean hasSymbolInThisArea(Word word) {
        return symbols.get(area).hasSymbol(word);
    } 
```

C: Check all areas. If the symbol has been defined, handle functions in the same manner.

```java
    private boolean hasSymbol(Word word) {
        for (Symbols s : symbols.values()) {
            if (s.hasSymbol(word)) {
                return true;
            }
        }
        return false;
    }
```

##### **d e**

To check if the function parameters are matched, the parameters of every function are memorized. When a function call is encountered, the function call parameters are scanned and matched. A function is prepared to handle this. It was found that recursive descent is needed again, so the check procedure is added to the recursive descent of the grammatical analyzer. Please check the `After Code/Error d and e`.

##### **f g**

There is a global variable `needReturn` used to indicate if the current function needs to return. If it does, but there is no return at the end of the code block, or if it doesn't, but there is a return, the error will be recorded.

##### **h**

Simply check if it is a constant.

```java
if (isConst(word)) {
  error("h", word.getLineNum());
}
```

##### **i j k**

Encapsulate the function for checking the missing symbol.

For example:

```java
    private void checkParent() {
        if (getNextWord().typeEquals("RPARENT")) {
            getWord();// )
        } else {
            error("j");
        }
    }
```

##### **l**

Count the number of parameters for `string` and `printf` separately and check if they are equal.

##### **m**

There is a global variable `whileFlag` indicating if the code block is within a while loop. If it isn't, any `continue` or `break` statements will produce an error.

### After Coding

#### Area

I increment `area++` when entering a block or a function, but this leads to a situation where the parameters of the function can't be memorized in a different area from the block of the function. Therefore, I changed the rules for marking `area++`.

```java
    private boolean analyseBlock(boolean fromFunc) {
        ...
        if (!fromFunc) {
            addArea();
        }
        ...
    }
```

Only when the block is not from the function, the area++.

#### Error d and e

To check if the function parameters are matched, an array is set for each function.

```java
public class Function {
    private String type;
    private String content;
    private String returnType;
    private ArrayList<Integer> paras;
}
```

When encountering a function, its return type and parameters are memorized.

For the `ArrayList<Integer> paras`, it reflects as follows:

| Type     | Example | Integer |
| -------- | ------- | ------- |
| Void     |         | -1      |
| Int      | a       | 0       |
| Int[]    | a[]     | 1       |
| Int[] [] | a[] [3] | 2       |

So, when encountering a function call, the parameters will be checked against what has been memorized before.

```java
private void checkParasMatchRParas(Word ident, ArrayList<Integer> paras, ArrayList<Integer> rparas) {
    if (paras.size() != rparas.size()) {
        error("d", ident.getLineNum());
    } else {
        for (int i = 0; i < paras.size(); i++) {
            if (!paras.get(i).equals(rparas.get(i))) {
                error("e", ident.getLineNum());
            }
        }
    }
}
```

To get the real type of the parameters, the analysis procedure is added to the recursive descent of the grammatical analyzer. For example:

```java
    private int analyseExp(ArrayList<Word> exp) {
        int intType = analyseAddExp(exp);
        grammar.add("<Exp>");
        return intType;
    }
```

Every recursion will return an `intType`, symbolizing the final type of the expression.

Because the terms of an expression must be of the same type, only one of them is returned.

This is the exit of the recursion. It will return the correct type of the expression to the top of the function.

```java
    private int analyseLVal(ArrayList<Word> exp) {
        int intType = 0;
        ...
                if (word.typeEquals("LBRACK")) {
                    intType++;
                    ...
                }
         ...
        if (hasSymbol(ident)) {
            return getSymbol(ident).getIntType() - intType;
        } else {
            return 0;
        }
    }
```

## Code Generation 代码生成 

In this part, I chose to generate Pcode.

I designed a type of Pcode which is an Inverse Polish notation expression stack and symbol table-based virtual code.

At the same time, I designed a virtual machine to execute it.

The Pcode virtual machine is an imaginary machine used to run Pcode commands. It consists of a code area (code), an instruction pointer (EIP), a stack, a var_table, a func_table, and a label_table.

In the following passage, I will first introduce how Pcode executes and then explain how to produce Pcode.

### Before Coding

#### How does the virtual machine run

First, we need a `codes` list and a `stack` (int).

An `eip` represents the address of the currently running code.

A `varTable` memorizes the address of the variable in the stack.

A `funcTable` memorizes the address of the function in the codes list.

A `labelTable` memorizes the address of the label in the codes list.

Then, run the code one after another and manage the stack.

#### How to distinguish different variables

Before generating codes, differentiate variables from different scopes by their unique scope number, like: `areaID + "_" + curWord.getContent()`. In this situation, a variable will not appear more than once in the codes, except for recursive function calls, which will be solved by pushing the `varTable` to the stack (shown later).

#### Specific Code Definition

First, define a class for PCode:

```java
public class PCode {
    private CodeType type;
    private Object value1 = null;
    private Object value2 = null;
}
```

It represents one code object, which has a `CodeType` and two operating values. `CodeType` is an enum. `Value1` and `Value2` may be Integer, String, or null, depending on the specific code type.

##### Calculation Type

Two operators:

```java
int b = pop();
int a = pop();
push(cal(a,b));
```

Single operator:

```java
push(cal(pop()));
```

##### VAR

The **VAR** command declares a variable, saving the variable name and the address assigned to it in the variable table.

```java
case VAR: {
    Var var = new Var(stack.size());
    varTable.put((String) code.getValue1(), var);
}
```

Var.class:

```java
public class Var {
    private int index;
    private int dimension = 0;
    private int dim1;
    private int dim2;
}
```

##### DIMVAR

The **DIMVAR** command declares an array, setting the dimension information of the variable.

```java
case DIMVAR: {
    Var var = getVar((String) code.getValue1());
    int n = (int) code.getValue2();
    var.setDimension(n);
    if (n == 1) {
        int i = pop();
        var.setDim1(i);
    }
    if (n == 2) {
        int j = pop(), i = pop();
        var.setDim1(i);
        var.setDim2(j);
    }
}
```

##### PLACEHOLDER

The **PLACEHOLDER** command grows the stack down, allocating new space for variables and arrays.

```java
case PLACEHOLDER: {
    Var var = getVar((String) code.getValue1());
    int n = (int) code.getValue2();
    if (n == 0) {
        push(0);
    }
    if (n == 1) {
        for (int i = 0; i < var.getDim1(); i++) {
            push(0);
        }
    }
    if (n == 2) {
        for (int i = 0; i < var.getDim1() * var.getDim2(); i++) {
            push(0);
        }
    }
}
```

##### Other

Calculation type: Pop the stack top once or twice, perform the calculation, and push the result back onto the stack.

Jump type: For jump commands, check if the condition is satisfied and change the `eip` accordingly.

Function call: As follows

#### Function call procedure

First, before the function call, parameters are pushed onto the stack. Each parameter push is followed by an `RPARA` command, which memorizes the address of the previous variable.

```java
case RPARA: {
    int n = (int) code.getValue1();
    if (n == 0) {
        rparas.add(stack.size() - 1);
    } else {
        rparas.add(stack.get(stack.size() - 1));
    }
}
```

Second, the function `CALL`.

Memorize the `eip`, stack top address, and information about the function (these will be pushed onto the stack as well). Then update the `varTable` and `eip`, preparing to execute the function.

```java
case CALL: {
    Func func = funcTable.get((String) code.getValue1());
    retInfos.add(new RetInfo(eip, varTable, stack.size() - 1, func.getArgs(), func.getArgs(), nowArgsNum));
    eip = func.getIndex();
    varTable = new HashMap<>();
    callArgsNum = func.getArgs();
    nowArgsNum = 0;
}
```

Finally, when it's `RET`, return.

Restore `eip` and `varTable` from `RetInfo`, and clear the new information pushed onto the stack during the function call.

```java
case RET: {
    int n = (int) code.getValue1();
    RetInfo info = retInfos.remove(retInfos.size() - 1);
    eip = info.getEip();
    varTable = info.getVarTable();
    callArgsNum = info.getCallArgsNum();
    nowArgsNum = info.getNowArgsNum();
    if (n == 1) {
        stack.subList(info.getStackPtr() + 1 - info.getParaNum(), stack.size() - 1).clear();
    } else {
        stack.subList(info.getStackPtr() + 1 - info.getParaNum(), stack.size()).clear();
    }
}
```

#### Value or Address

Pushing the value or address of a variable is important and depends on the specific need, which will be explained when I describe how to generate codes.

The command action is as follows (`getAddress` is used to get the address of the previous variable).

```java
case VALUE: {
    Var var = getVar((String) code.getValue1());
    int n = (int) code.getValue2();
    int address = getAddress(var, n);
    push(stack.get(address));
}
...
case ADDRESS: {
    Var var = getVar((String) code.getValue1());
    int n = (int) code.getValue2();
    int address = getAddress(var, n);
    push(address);
}
```

#### Code Generation

Code is generated from the grammatical analysis procedure.

##### Declaration

There is no need to distinguish between constants and variables. When declaring a variable, create a new variable and let it point to the stack top. If it has an initialization, push the values one after another. If not, add a `PLACEHOLDER` command to push something (I push 0) to the stack to hold the place.

##### Assignment Sentence

In this situation, first calculate and push the address of the variable to the stack top. Then analyze the expressions. After that, there will only be two numbers in the stack: the address and the value. Assign the value to the address.

##### Condition Control Sentence

First, generate labels. Then, place jump sentences in the proper places.

Labels for `if` and `while` will be generated and then stored in a stack-like structure, like:

```java
whileLabels.add(new HashMap<>());
whileLabels.get(whileLabels.size() - 1).put("while", labelGenerator.getLabel("while"));
whileLabels.get(whileLabels.size() - 1).put("while_end", labelGenerator.getLabel("while_end"));
whileLabels.get(whileLabels.size() - 1).put("while_block", labelGenerator.getLabel("while_block"));
```

Take `if` as an example:

```java
if (word.typeEquals("IFTK")) {
    codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if")));
    ...
    analyseCond("IFTK");
    ...
    codes.add(new PCode(CodeType.JZ, ifLabels.get(ifLabels.size() - 1).get("else")));
    codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if_block")));
    analyseStmt();
    codes.add(new PCode(CodeType.JMP, ifLabels.get(ifLabels.size() - 1).get("if_end")));
    codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("else")));
    if (word.typeEquals("ELSETK")) {
        getWord(); //else
        analyseStmt();
    }
    codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if_end")));
}
```

while:

```java
if (word.typeEquals("WHILETK")) {
    ...
    codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while")));
    ...
    analyseCond("WHILETK");
    ...
    codes.add(new PCode(CodeType.JZ, whileLabels.get(whileLabels.size() - 1).get("while_end")));
    codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while_block")));
    analyseStmt();
    ...
    codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while")));
    codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while_end")));
    whileLabels.remove(whileLabels.size() - 1);
}

// break
if (word.typeEquals("BREAKTK")) {
    getWord();//break
    codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while_end")));
 		...
}

// continue
if (word.typeEquals("CONTINUETK")) {
    getWord();//continue
    codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while")));
    ...
} 
```

### After Coding

Because of some runtime errors and information shortages, I added and removed some Pcode. At the same time, there are some new troubles with address passing and short-circuit calculation.

#### Specific Code Definition

In operations, `push()` means putting a value onto the top of the stack. `pop()` means removing the value from the top of the stack.

##### Common Type

| CodeType    | Value1              | Value2             | Operation                               |
| ----------- | ------------------- | ------------------ | --------------------------------------- |
| LABEL       | Label_name          | Set  a label       |                                         |
| VAR         | Ident_name          | Declare  a variety |                                         |
| PUSH        | Ident_name/Digit    | push(value1)       |                                         |
| POP         | Address             | Ident_name         | *value1 = value2                        |
| JZ          | Label_name          |                    | Jump if stack top is zero               |
| JNZ         | Label_name          |                    | Jump if stack top is not zero           |
| JMP         | Label_name          |                    | Jump unconditionally                    |
| MAIN        |                     |                    | Main function label                     |
| FUNC        |                     |                    | Function label                          |
| ENDFUNC     |                     |                    | End of function label                   |
| PARA        | Ident_name          | Type               | Parameters                              |
| RET         | Return value or not |                    | Function return                         |
| CALL        | Function name       |                    | Function call                           |
| RPARA       | Type                |                    | Get parameters ready for function call  |
| GETINT      |                     |                    | Get a integer and put it into stack top |
| PRINT       | String              | Para num           | Pop values and print.                   |
| DIMVAR      | Ident_name          | Type               | Set dimension info for array variety    |
| VALUE       | Ident_name          | Type               | Get the variety value                   |
| ADDRESS     | Ident_name          | Type               | Get the variety address                 |
| PLACEHOLDER |                     |                    | Push something to hold places           |
| EXIT        |                     |                    | Exit                                    |

| CodeType | Value1 | Value2 | Operation |
| -------- | ------ | ------ | --------- |
| ADD      |        |        | +         |
| SUB      |        |        | -         |
| MUL      |        |        | *         |
| DIV      |        |        | /         |
| MOD      |        |        | %         |
| CMPEQ    |        |        | ==        |
| CMPNE    |        |        | !=        |
| CMPGT    |        |        | >         |
| CMPLT    |        |        | <         |
| CMPGE    |        |        | >=        |
| CMPLE    |        |        | <=        |
| AND      |        |        | &&        |
| OR       |        |        | \|\|      |
| NOT      |        |        | !         |
| NEG      |        |        | -         |
| POS      |        |        | +         |

#### short circuit calculation

There are two situations where the short-circuit calculation is needed:

```c
1. if(a&&b) // a is false
2. if(a||b) // b is true
```

This was not an easy task, and I actually spent a lot of time solving it.

Here is my method:

First, when analyzing `analyseLOrExp`, every `analyseLAndExp` is followed by a `JNZ`, which is used to detect if the condition is false. If it is, it jumps to the if body label. At the same time, I generated a condition label, which is ready for the `analyseLAndExp`.

```java
private void analyseLOrExp(ArrayList<Word> exp, String from) {
    ...
    for (...) {
        ...
        String label = labelGenerator.getLabel("cond_" + i);
        analyseLAndExp(exp1, from, label);
        codes.add(new PCode(CodeType.LABEL, label));
        if (...) {
            codes.add(new PCode(CodeType.OR));
        }
        if (...) {
            if (...) {
                codes.add(new PCode(CodeType.JNZ, ifLabels.get(ifLabels.size() - 1).get("if_block")));
            }
          ...
        }
        ...
    }
}
```

In the `analyseLAndExp`, every `analyseEqExp` is followed by a `JZ`, which is used to detect if the condition is true. If it is, it jumps to the condition label I set earlier.

```java
private void analyseLAndExp(ArrayList<Word> exp, String from, String label) {
    ...
    for (...) {
        ...
        analyseEqExp(exp1);
        if (...) {
            codes.add(new PCode(CodeType.AND));
        }
        if (...) {
            if (...) {
                codes.add(new PCode(CodeType.JZ, label));
            } 
          ...
        }
    }
}
```

By these means, short-circuit evaluation is achieved.

## 总结感想 Summary

先说说关于选择PCODE，选择了PCODE，早早结束了编译实验的全部任务，也没经过很多人在选择mips85+还是选择PCODE+85的痛苦，私以为我不愿陷入焦虑去纠结给分高低，也不想花大量时间去研究怎么生成mips和卷优化，因为这不是我想做和喜欢的事情。当然，生成PCODE反而是我感觉更能让我觉得有趣的事情，无需遵循某种既定的体系（虽然这可能对我没有好处），而是自己设计了PCODE指令，一边从递归下降中生成指令，一边写虚拟机解释器，在这之间，我不断修改指令生成和解释器，感觉自己能够完全地掌控这个我亲手写的编译器，最后让虚拟机完整地成功地跑代码，反而对我来说是一件很有成就感的事情。

至于这个文档为什么要写成英文，首先，想要更方便更直观地描述代码，类，对象，与其给每一个东西一个汉语解释，我认为干脆用英文来描述更显自然，第二，想在写文档的同时顺便练练英语，仅此而已，不是想给助教和看我文档的人添麻烦，如果很不方便阅读，可以翻译成汉语再读，用有道翻译完还是可以看懂的，实在不行也可以来垂询我本人。总结当然就不写英语了。

关于一些反思，在语法分析和代码生成两部分，我还是没有设计好架构就开始写，完工后出现了较多bug，甚至在代码生成de到了语法分析时候的bug，在面向对象课程的时候，已经多次反思过这件事，这实属不该，在今后的大型工程中，要继续好好注意架构设计。也非常感谢在我debug过程中帮到我的zyq，lyx，dky同学。

最后，站在我个人的角度，我认为从词法分析，语法分析，错误处理，到代码生成这四个任务，已经很好的做到了让我对一个编译器体系架构充分理解并让我把它完整的做出来，对我个人而言，这已经完整了，我算是轻松愉快地（除了在语法分析和代码生成两次任务debug的时候属实不太顺利）完成了编译实验，很有成就感，且收获颇丰。
