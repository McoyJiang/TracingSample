# TracingSample
Android tracing letters using Canvas & Paint

there are two modes of Tracing: default and instruction mode

## Instruction Mode

<img src='https://github.com/McoyJiang/TracingSample/blob/master/images/ins2.gif' width='350' height='200'>

## Default

<img src='https://github.com/McoyJiang/TracingSample/blob/master/images/unins.gif' width='350' height='200'>


# how to use
#### 1. step 1 download tracinglibrary, and import it as library module

```
implementation project(path: ':tracinglibrary')
```

#### 2. in Activity layout xml, declare 
```xml
<com.danny_jiang.tracinglibrary.view.TracingLetterView
        android:id="@+id/letter"
        android:layout_width="wrap_content"
        android:layout_height="0dp"
        app:pointColor="@color/colorAccent"
        app:instructionMode="false"
        app:strokeColor="@color/colorPrimary"
        app:anchorDrawable="@drawable/star"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintHeight_percent="0.8"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
```
#### 3. you can set different attr for TracingLetterView
```xml
  app:pointColor="@color/colorAccent"
  app:instructionMode="false"
  app:strokeColor="@color/colorPrimary"
  app:anchorDrawable="@drawable/star"
```  

#### 4. set which letter you want to trace
```
letterView = findViewById(R.id.letter);
letterView.setLetterChar(LetterFactory.A);
```

#### 5. set propertise in Activity dynamically

```java
    letterView.setPointColor(Color.BLUE);
    letterView.setInstructMode(true);    
```



#### 6. set tracing progress listener
```
letterView.setTracingListener(new TracingLetterView.TracingListener() {
            @Override
            public void onFinish() {
                Toast.makeText(MainActivity.this,
                        "tracing finished", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTracing(float x, float y) {
                Log.e("ABC", "tracing x : " + x + " y : " + y);
            }
        });
```
