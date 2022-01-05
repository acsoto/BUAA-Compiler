可切换分支查看每个设计阶段的代码

期末上机考核题目：https://atksoto.com/关于编译实验期末上机考试/

## 词法分析

### Before Code

Requriement:  read `testfile.txt`, parse every char to word and print them. At the same time, memorize type, content and line number of  each word.

#### File reading

Read by line, scan every char of every string and analyse.

```java
while ((s = bf.readLine()) != null) {
	...
}
```

#### Analyse

When i get the key word, enter the next analyst. 

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

For example, when I get '+', I directly new a Word typify the "PLUS".

##### Function

For example

When I get `<` , enter function`analyseRelation` read one char more. If it is`=`, analyze `LEQ`...

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

Digit: When I get a digit, it means I will scan a serial of some digits and turn them into a Word typify "INTCON".

Letter: When I get a letter, it means I will scan a string about letter or digit. It maybe a "IDENFR" or "STRCON", which depends on whether it is in key map or not. 

#### Word

class Word:

```java
public class Word {
    private String identification;
    private String content;
    private String type;
}
```

Capsulate the initial function, I only need to `new Word(...)` in the main processor, which will create the corresponding word.

For example

```java
    public Word(char identification) {
        this.identification = String.valueOf(identification);
        this.type = new KeyWordMap().getType(this.identification);
        this.content = this.identification;
    }
```

As for KeyWordMap, it is a HashMap, mapping the string of word and its type.

```java
    public KeyWordMap() {
        keyWords = new HashMap<>();
        keyWords.put("main", "MAINTK");
        keyWords.put("const", "CONSTTK");
        keyWords.put("int", "INTTK");
        ...
        }
```

### After Code

#### File reading

Read file by line is not convenient for preread and undo, so I read the file into a single String at first. 

The method is read by line, add `\n` after every line and scan every char. When I get `\n`, `lineNum++`

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

About analyst, it is different from what before coding.

First, I need analyze word one by one, so I add global variety `index` to memorize where is the pointer. 

Besides, I met the situation that I need read one more or undo, so I  encapsulate the function `ungetChar` and `getChar`, which will be convenient for me to analyze. 

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

2) `/* */`: Get char until `*/` appears

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

## 语法分析

Requirement: Based on the words identified by the lexical analysis program, identify various grammatical elements according to the grammatical rules. Recursive descent method is used to analyze the grammatical components defined in the grammar.

### Before Code

#### Data Reading

Like the lexical analyst, I prepared function  `getWord` `getNextWord` and so on. At the same time, there is a global variety `(Word) curWord` to display which word it is when I read `ArrayList<Word> words ` from lexical analyst one by one.    

My analyst tragedy is as follows:

To normal rule: I keep getting words and analyze them

To expression rule: I scan the whole expression first, which is implemented by function `getExp`. Then I divide the expression and use method recursive descent to analyze them.

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

According to Grammatical Rules, code function for every term of rule.

Main idea: read a word, check what it symbolize and enter the next analyzing function.  

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

grammar is used for memorize output of lexical analyst and grammar analyst list. 

#### left recursion

```java
加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp // 1.MulExp 2.+ 需覆盖 3.- 需覆盖
```

Check if the exp has '+' or '-'. If it has, separate the exp to AddExp and  MulExp. Then analyze them separately. 

### After Code

#### left recursion

The method used before is not perfect for recursive descent. So I changed my rewrite way.

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

Function `divideExp` is used for divide the whole exp passed by `getExp` or the pre function.

`divideExp`:

In: orignal: `exp` stop symbol:  `symbol`

Out: List of divided exp and symbol.

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

Most bugs are produced by function `getExp` and `divideExp` because of some situations are ignored. So I always get something like index out of range...

So I changed some symbol of stop getting expression and modify the rules to divide or not the expression and so on.

## 错误处理

### Before Code

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

Type means the type of the symbol.

IntType is an integer. If it's 0, the symbol is int. if it's 1, the symbol is int[],  if it's 2, the symbol is int[] []...

Content is its content.

Area is where is it.

I create a HashMap of Symbols, memorizing symbols created in each area.

When I enter a new area, area++. When I leave an area, area--, with the corresponding Symbols are destroyed. 

```java
    private HashMap<Integer, Symbols> symbols = new HashMap<>();
    private HashMap<String, Function> functions = new HashMap<>();
    private ArrayList<Error> errors = new ArrayList<>();
    private int area = -1;
    private boolean needReturn = false;
    private int whileFlag = 0;
```

`needReturn` means if the current function need to return.

`whileFlag ` means if the current code block is in while circle.

#### Errors

##### **a**

Just check format

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

B: Every time I get an identity, check if there is the same symbol has been defined in this area.

```java
    private boolean hasSymbolInThisArea(Word word) {
        return symbols.get(area).hasSymbol(word);
    } 
```

C: Check all area. If the symbol has been defined. Functions are as the same.

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

To check if the function parameters are matched, I memorize parameters of every function and when I met a function call, I will scan the function call parameters and match them. I prepare a function to do this. Finally I found I need to use recursive descent again, so I add the check procedure to the recursive descent of the grammatical analyst. Please check the `After Code/Error d and e`

##### f g

There is a global variety `needReturn` used to display if the current function need return. if it does but there is no return in the end of the code block, or if it doesn't but there is return, the error will be memorized.

##### **h**

Just check if it is a const. 

```java
if (isConst(word)) {
  error("h", word.getLineNum());
}
```

##### **i j k**

Capsulate function about checking missing of the symbol

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

Count the number of the parameters of string and printf separately and check if they equal.

##### **m**

There is a global variety `whileFlag` symbolize if the code block is in while circle. If it isn't, any continue and break will produce error.

### After Code

#### Area

I mark the area++ when I get a block or a function, but it will lead to the situation that when enter a code block of a function, the parameters of the function can't be memorize in the different are with the block of the function. So I changed the rules to mark area++.

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

To check if the function parameters are matched, I set an array for every function.

```java
public class Function {
    private String type;
    private String content;
    private String returnType;
    private ArrayList<Integer> paras;
}
```

When I get a function, I memorize its return type and paras.

As for the `ArrayList<Integer> paras`, it reflects as follows:

| Type     | Example | Integer |
| -------- | ------- | ------- |
| Void     |         | -1      |
| Int      | a       | 0       |
| Int[]    | a[]     | 1       |
| Int[] [] | a[] [3] | 2       |

So when I get a function call, I will check the parameter of it with what I have memorized before.

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

As for getting the parameters real type, I add the analyst procedure to the recursive descent of the grammatical analyst. Just like:

```java
    private int analyseExp(ArrayList<Word> exp) {
        int intType = analyseAddExp(exp);
        grammar.add("<Exp>");
        return intType;
    }
```

Every recursion will return an `intType`, which symbolize the final type of the expression.

Because the terms of one expression must be the same type, so I return only one of them.

This is the exit of the recursion. It will return a correct type of the expression to the top of the function.

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

## 代码生成

In this part, I chose to generate Pcode.

I designed a type of Pcode which is an Inverse Bolan expression stack and symbol table based virtual code.

At the same time, I designed virtual machine to execute them.

The Pcode virtual machine is an imaginary machine used to run Pcode commands. It consists of: A code area (code), an instruction pointer (EIP), a stack, a var_table, a func_table and a label_table.

In the following passage, I will introduce how Pcode executes first and how to produce Pcode next.

### Before Code

#### How does the virtual machine run

First, we need a `codes` list and a `stack`(int).

An `eip`: presents the address of current running code.

A `varTable`: memorizes the address of the variety in stack.

A `funcTable`: memorizes the address of the function in codes list.

A `labelTable`: Memorizes the address of the label in codes list.

Then, run the code one after another and manage the stack.

#### How to distinguish different variety

Before generate codes, differentiate varieties from different scopes by its only scope number, like: `areaID + "_" + curWord.getContent()`. In this situation, the variety will not appear more than once in codes, except for recursive function call, which will be solved by push `varTable` to stack(show later).

#### Specific Code Definition

First, define a class for PCode:

```java
public class PCode {
    private CodeType type;
    private Object value1 = null;
    private Object value2 = null;
}
```

It presents one code object, which has a CodeType and two operating values. CodeType is an enum. Value1 and value2 maybe Integer or String or null, which depends on specific code type.

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

**VAR** command to declare a variable, save the variable name and the address assigned to it in the variable table.

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

**DIMVAR** command to declare an array. Set the dimension information of the var.

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

**PLACEHOLDER** command to grow the stack down, allocate the new space to the variety and array.

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

Calculation type: pop the stack top once or twice, calculate them and push again.

Jump Type: When it's command about jump, just check if the condition is satisfied and change the `eip`.

Function call: as follows

#### Function call procedure

First, before function call, there will be some parameters to be pushed into the stack. Each will be followed by a `RPARA` command, which memorize the address of the previous variety. 

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

Second, function `CALL`.

Memorize the eip, stack top address, and information about the function(In fact, they will be pushed into stack too). Then update the `varTable` and `eip`.  Ready for execute function. 

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

Finally, return when it's `RET`

Restore `eip`, `varTable` from `RetInfo`, clear the new information pushed when function in the stack.

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

Push value or address of the variety is an important thing, it depends on what I need, which will be presented when I describe how to generate codes.

The command action is as follows(`getAddress` is used for get the address of the previous variety ).

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

#### Code Generate

Code generated from the grammatical analyst procedure.

##### Declaration

There is no need to distinguish const and var. When declare a variety, just new a variety and let it point to the stack top. Then if it has an initialization, just push the values one after another. If not, add a `PLACEHOLDER` command to push something(I push 0) to the stack to hold the place.

##### Assign sentence

In this situation, first calculate and push the address of the variety to the stack top. Then analyze expressions. After that, there are only two number in the stack, which are address and value. Assign the value to the address.

##### Condition control sentence

First, generate labels. Then, place jump sentences in the proper places.

labels about if and while will be generated and then stored in a stack type structure. like:

```java
whileLabels.add(new HashMap<>());
whileLabels.get(whileLabels.size() - 1).put("while", labelGenerator.getLabel("while"));
whileLabels.get(whileLabels.size() - 1).put("while_end", labelGenerator.getLabel("while_end"));
whileLabels.get(whileLabels.size() - 1).put("while_block", labelGenerator.getLabel("while_block"));
```

Take if as example:

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

### After Code

Because of some runtime errors and information shortages, I added and removed some Pcode. At the same, there are some new troubles about address pass and short circuit calculation.

#### Specific Code Definition

In Operation, `push()` means put value into the top of the stack. `pop()` means pop the value from the top of the stack.

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

There are two situations I need to use short circuit calculation :

```c
1. if(a&&b) // a is false
2. if(a||b) // b is true
```

This seems not an easy thing and I acutally spent lots of time to solve it.

My method is as follows:

First, when I analyze `analyseLOrExp`, every `analyseLAndExp` will be followed by a `JNZ`, which is used for detect if the cond is false. If it is, jump to the if body label. At the same time, I generated cond label, which is ready for the `analyseLAndExp`.

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

In the `analyseLAndExp`, every `analyseEqExp` will be followed by a `JZ`, which is used for detect if the cond is true. If it is, jump to the cond label I set just now.

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

By these means, short circuit calculation is solved.

## 总结感想

先说说关于选择PCODE，选择了PCODE，早早结束了编译实验的全部任务，也没经过很多人在选择mips85+还是选择PCODE+85的痛苦，私以为我不愿陷入焦虑去纠结这个什么给分高低，也不想浪费大量时间去研究怎么生成mips和卷优化，因为这不是我想做和喜欢的事情。也不为别的，就是想要轻松一点，节省时间做点自己想做的事情。当然，生成PCODE反而是我感觉更能让我觉得有趣的事情，无需遵循某种既定的体系（虽然这可能对我没有好处），而是自己设计了PCODE指令，一边从递归下降中生成指令，一边写虚拟机解释器，在这之间，我不断修改指令生成和解释器，感觉自己能够完全地掌控这个我亲手写的编译器，最后让虚拟机完整地成功地跑代码，反而对我来说是一件很有成就感的事情。

至于这个文档为什么要写成英文，首先，想要更方便更直观地描述代码，类，对象，与其给每一个东西一个汉语解释，我认为干脆用英文来描述更显自然，第二，想在写文档的同时顺便练练英语，仅此而已。不是想给助教和看我文档的人添麻烦，如果很不方便阅读，可以翻译成汉语再读，亲测用有道翻译完还是可以看懂的，实在不行也可以来垂询我本人。总结当然就不写英语了，毕竟不是母语。

关于一些反思，在语法分析和代码生成两部分，我还是没有设计好架构就开始写，完工后出现了较多bug，甚至在代码生成de到了语法分析时候的bug，在面向对象课程的时候，已经多次反思过这件事，这实属不该，在今后的大型工程中，要继续好好注意架构设计。也非常感谢在我debug过程中帮到我的zyq，lyx，dky同学。

最后，站在我个人的角度，我认为从词法分析，语法分析，错误处理，到代码生成这四个任务，已经很好的做到了让我对一个编译器体系架构充分理解并让我把它完整的做出来，对我个人而言，这已经完整了，我算是轻松愉快地（除了在语法分析和代码生成两次任务debug的时候属实不太顺利）完成了编译实验，很有成就感，且收获颇丰。
