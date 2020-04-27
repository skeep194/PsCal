package com.skeep.pscal

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.skeep.pscal.databinding.FragmentCalculatorBinding
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*

class Calculator : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentCalculatorBinding
    lateinit var calVar: Bundle

    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {
        binding.expressionText.requestFocus()
        val exp = binding.expressionText
        val cursor = exp.selectionEnd
        when(v?.id) {
            R.id.cal -> binding.resultText.text = when(binding.changeType.text) {
                "int" -> calculateInt() ?: getString(R.string.syntax_error)
                "float" -> calculateFloat() ?: getString(R.string.syntax_error)
                else -> "unknown error"
            }
            R.id.cancel -> {
                if(exp.text.isNotEmpty() && cursor != 0) {
                    if(exp.text[cursor-1] in 'a'..'z' || exp.text[cursor-1] == '_' || exp.text[cursor-1] == '<' || exp.text[cursor-1] == '>') {
                        var start = cursor - 1
                        var end = cursor - 1
                        while(end < exp.text.length && (exp.text[end] in 'a'..'z' || exp.text[end] == '_' || exp.text[end] == '<' || exp.text[end] == '>')) {
                            ++end
                        }
                        while(start >= 0 && (exp.text[start] in 'a'..'z' || exp.text[start] == '_' || exp.text[start] == '<' || exp.text[start] == '>')) {
                            --start
                        }
                        exp.text.delete(start+1,end)
                    }
                    else {
                        exp.text.delete(cursor-1,cursor)
                    }
                }
            }
            R.id.allClear ->  {
                exp.setText("")
                binding.resultText.text = ""
            }
            R.id.variable -> {
                val popup = CalculatorVal()
                popup.arguments = calVar
                popup.show(parentFragmentManager,"dialog_setVariable")

                calVar = popup.arguments!!
            }
            else -> {
                if(v is TextView) {
                    when(v.text) {
                        "x^n" -> exp.text.insert(cursor,"^")
                        "nCr" -> exp.text.insert(cursor, "C")
                        "log" -> exp.text.insert(cursor,"log_2(")
                        "x" -> exp.text.insert(cursor,"*")
                        "gcd" -> {
                            exp.text.insert(cursor, "gcd(,")
                            exp.setSelection(exp.selectionEnd-1)
                        }
                        "int" -> {
                            v.text = "float"
                        }
                        "float" -> {
                            v.text = "int"
                        }
                        else -> exp.text.insert(cursor,v.text)
                    }
                }
            }
        }
    } //버튼 클릭 이벤트 설정

    enum class Priority(private val p: Int) {
        UNARY(-1), PLUS(6), MINUS(6), MULTIPLY(4),
        DIVIDE(4), MODULAR(4), OPENPARENTHESIS(99), POWER(2),
        RIGHTSHIFT(7), LEFTSHIFT(7), BITWISEAND(10), COMBINATION(5),
        BITWISEOR(12), BITWISEXOR(11);
        fun toInt() = p

    } //계산시 연산자 우선순위 정의

    private fun getPriority(operator: String) = when(operator) {
        "+" -> Priority.PLUS.toInt()
        "-" -> Priority.MINUS.toInt()
        "*" -> Priority.MULTIPLY.toInt()
        "/" -> Priority.DIVIDE.toInt()
        "(" -> Priority.OPENPARENTHESIS.toInt()
        ">>" -> Priority.LEFTSHIFT.toInt()
        "<<" -> Priority.RIGHTSHIFT.toInt()
        "^" -> Priority.POWER.toInt()
        "and" -> Priority.BITWISEAND.toInt()
        "or" -> Priority.BITWISEOR.toInt()
        //"not" -> Priority.BITWISENOT.toInt()
        "xor" -> Priority.BITWISEXOR.toInt()
        "mod" -> Priority.MODULAR.toInt()
        "C", "P" -> Priority.COMBINATION.toInt()
        else -> Priority.UNARY.toInt()
    } //우선순위 객체 -> Int 변환 함수

    private fun tokenSplit(operator: String): String = when(operator[0]) {
        in 'a'..'z' -> if(operator.length >= 2 && operator[1] == 'r') (if(operator.length >= 2) operator.substring(0,2) else "?") else (if(operator.length >= 3) operator.substring(0,3) else "?")
        '>' -> ">>"
        '<' ->  "<<"
        in '0'..'9' -> {
            var index = 0
            while(index < operator.length && operator[index] in '0'..'9') ++index
            if(index < operator.length && operator[index] == '.') {
                ++index
                while(index < operator.length && operator[index] in '0'..'9') ++index
            }
            operator.substring(0,index)
        }
        else -> operator[0].toString()
    } //문자열 받아서 가장 앞에 있는 토큰을 분리해서 반환함

    private fun parser(operator: String): Queue<String>? {
        var index = 0
        val ret: Queue<String> = LinkedList()
        val check = ArrayList<String>()
        while(index < operator.length) {
            var token = tokenSplit(operator.substring(index))
            if(token.isEmpty()) {
                return null
            }
            if(( token == "+" || token == "-" ) && (check.isEmpty() || ((check.last()[0] !in '0'..'9') && check.last() != "!" && check.last() != ")"))) {
                token = if(token == "+") "#" else "_"
            } //+, -에 대한 부호표현 처리


            check.add(token)

            index += token.length
        }

        for(i in check) ret.offer(i)
        return if(isCorrectSyntax(check)) ret else null
    }
    /*
        현재 파서 버전 v0.2
        계산을 하는 어떤 단계에서든 에러발견 시 null 반환
        ,를 특정 연산자(gcd, lcm)으로 치환하는 작업 필요함 : Beta(v0.1) 에선 미구현

        부호 처리 규칙(v0.1)
        +, - 앞에 아무것도 없거나 다른 연산자가 있을 경우 부호 표현으로 인식

        계산 과정 문법 에러 규칙(v0.2)
        계산하다 스택 부서지면 에러
        괄호 짝 안맞는 경우 별도 처리
        잘못된 토큰 처리
        파서가 파싱할 수 없는 경우에도 에러

        알려진 이슈
        매우 큰 숫자 인풋에 대해 예외처리 없음 -> 해결, 어차피 계산기 사용자가 오버플로우를 의도할 일은 없다고 판단하여 정수형을 BigInteger 로 계산
        ! 계산이 O(N)이라서 1억보다 높은 수의 인풋에 대해 결과가 나오지 않거나 매우 느리게 나옴, 근데 이건 어케할 방법이 없다..
        !3 = 6 이런 문법이 허용됨
        반대로 9not = -10 이런거도 허용됨
        (원인 : 단항 연산자의 결합 범위 지정하지 않음, 현재는 숫자나 식 앞이나 뒤에 단항 연산자가 올 경우 무조건 높은 우선순위로 처리해버림)
    */
    private val tokenList = arrayOf("+","-","*","/","#","_","C","^","not","xor","and","or","<<",">>","mod","gcd","(",")","!","log")
    private fun isCorrectToken(check: String) : Boolean {
        if(check in tokenList) return true
        else {
            var decimalPoint = false
            for(i in check) if(i !in '0'..'9') if(binding.changeType.text == "float" && !decimalPoint) decimalPoint = true else return false
            return true
        }
    }

    private fun isCorrectSyntax(check: ArrayList<String>) : Boolean {
        for(i in check) if(!isCorrectToken(i)) return false
        return true
    }//파서가 분리한 토큰을 가지고 올바른 구문인지 체크함, 규칙은 파서 설명 참조

    private infix fun BigInteger.pow(other: BigInteger): BigInteger =
        if(other == BigInteger.ZERO) BigInteger.ONE
        else if(other % BigInteger("2") == BigInteger.ONE) this * (this pow (other-BigInteger.ONE))
        else{
            val half = this pow (other/BigInteger("2"))
            half * half
        } //계산기 내부구현용 제곱 함수

    private fun factorial(num: BigInteger): BigInteger {
        if(num > BigInteger("1000")) {
            return BigInteger("-1")
        }
        if(num == BigInteger.ZERO) return BigInteger.ONE
        var ret = BigInteger.ONE
        for(i in 1..num.toInt()) {
            ret *= BigInteger(i.toString())
        }
        return ret
    }

    private infix fun BigInteger.comb(other: BigInteger): BigInteger = if(other > this) BigInteger.ZERO else factorial(this) / (factorial(this-other) * factorial(other))

    private fun isDecimal(check: String): Boolean = when(check[0]) {
        in '0'..'9' -> true
        'N','M','X','Y','Z' -> true
        'e' -> true
        else -> false
    } //토큰이 피연산자인지 여부 반환, 이 때 들어오는 문자열은 항상 올바르다고 가정 (문자열 전처리를 끝낸 후 이므로)

    private fun toDecimal(converter: String): String {
        return converter
    }

    private fun calculateInt(): String? {
        val postfix = toPostfix() ?: return null
        //val type = binding.changeType.text
        val calStack = Stack<BigInteger>()
        while(postfix.isNotEmpty()) {
            val here = postfix.poll() ?: return null
            when {
                isDecimal(here) -> calStack.push(BigInteger(toDecimal(here)))
                getPriority(here) == -1 -> {
                    val operand = if(calStack.isNotEmpty()) calStack.pop() else return null
                    calStack.push(when(here) {
                        "#" -> operand
                        "_" -> operand*BigInteger("-1")
                        "not" -> operand.inv()
                        "!" -> factorial(operand)
                        else -> return null
                    })
                }
                else -> {
                    val operand2 = if(calStack.isNotEmpty()) calStack.pop() else return null
                    val operand1 = if(calStack.isNotEmpty()) calStack.pop() else return null
                    calStack.push(when(here) {
                        "+" -> operand1 + operand2
                        "-" -> operand1 - operand2
                        "*" -> operand1 * operand2
                        "/" -> if(operand2 != BigInteger.ZERO) operand1 / operand2 else return getString(R.string.divide_by_zero)
                        "and" -> operand1 and operand2
                        "or" -> operand1 or operand2
                        "xor" -> operand1 xor operand2
                        "mod" -> operand1 % operand2
                        "^" -> operand1 pow operand2
                        ">>" -> operand1 shr operand2.toInt()
                        "<<" -> operand1 shl operand2.toInt()
                        "C" -> operand1 comb operand2
                        else -> return null
                    })
                }
            }
        }
        return if(calStack.size == 1) calStack.pop().toString() else null
    } //후위 연산식을 계산 후 결과값 문자열 반환, 현재 BigInteger 형에 대해 계산하는 걸로 되어있으나, 각 자료형에 대한 함수를 따로 구현해야 할 것 같음

    private fun calculateFloat(): String? {
        val postfix = toPostfix() ?: return null
        //val type = binding.changeType.text
        val calStack = Stack<BigDecimal>()
        while(postfix.isNotEmpty()) {
            val here = postfix.poll() ?: return null
            when {
                isDecimal(here) -> calStack.push(BigDecimal(toDecimal(here)).setScale(1000))
                getPriority(here) == -1 -> {
                    val operand = if(calStack.isNotEmpty()) calStack.pop() else return null
                    calStack.push(when(here) {
                        "#" -> operand
                        "_" -> operand*BigDecimal("-1")
                        //"not" -> operand.inv()
                        //"!" -> factorial(operand)
                        else -> return null
                    }.setScale(1000))
                }
                else -> {
                    val operand2 = if(calStack.isNotEmpty()) calStack.pop() else return null
                    val operand1 = if(calStack.isNotEmpty()) calStack.pop() else return null
                    calStack.push(when(here) {
                        "+" -> operand1 + operand2
                        "-" -> operand1 - operand2
                        "*" -> operand1 * operand2
                        "/" -> if(operand2 != BigDecimal.ZERO) operand1 / operand2 else return getString(R.string.divide_by_zero)
                        //"and" -> operand1 and operand2
                        //"or" -> operand1 or operand2
                        //"xor" -> operand1 xor operand2
                        //"mod" -> operand1 % operand2
                        //"^" -> operand1 pow operand2
                        //">>" -> operand1 shr operand2.toInt()
                        //"<<" -> operand1 shl operand2.toInt()
                        //"C" -> operand1 comb operand2
                        else -> return null
                    }.setScale(1000))
                }
            }
        }
        return if(calStack.size == 1) calStack.pop().setScale(6,RoundingMode.HALF_UP).toString() else null
    } //후위 연산식을 계산 후 결과값 문자열 반환, BigDecimal 형으로 연산
    //소수점 정밀도 사용자가 입력 가능하게 해야 함


    private fun toPostfix(): Queue<String>? {
        val exp = binding.expressionText.text
        val ret: Queue<String> = LinkedList()
        val parse = parser(exp.toString())
        val expr = Stack<String>()
        var cntOpenParenthesis = 0

        while(parse?.isNotEmpty() ?: return null) {
            val token = parse.poll() ?: break
            if(token[0] in '0'..'9') {
                ret.offer(token)
                continue
            }
            if(token[0] == '(') {
                expr.push(token)
                ++cntOpenParenthesis
                continue
            }
            if(token[0] == ')') {
                if(cntOpenParenthesis == 0) return null
                while(expr.peek() != "(") {
                    ret.offer(expr.pop())
                }
                expr.pop()
                --cntOpenParenthesis
                continue
            }

            while(expr.isNotEmpty() && (getPriority(expr.peek()) <= getPriority(token))) {
                if(getPriority(expr.peek()) == -1 && getPriority(token) == -1) {
                    break
                }
                ret.offer(expr.pop())
            }
            expr.push(token)
        }
        while(expr.isNotEmpty()) {
            ret.offer(expr.pop())
        }
        /*var debugLog: String = ""
        while(ret.isNotEmpty()) {
            debugLog += ret.poll()
        }
        Log.d("toPostfixDebug",debugLog) //디버그용 로그 찍는 문장, 정식 버전 때 삭제 예정*/
        return ret
    } //중위 연산식을 후위 연산으로 변환 후 반환

    private fun setExpressionText() {
        binding.expressionText.setTextIsSelectable(true)
        binding.expressionText.showSoftInputOnFocus = false
        binding.expressionText.setOnLongClickListener { true }
        binding.expressionText.customSelectionActionModeCallback = object:ActionMode.Callback {
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return false
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
            }
        }
        binding.expressionText.isSuggestionsEnabled

    } //수식 입력 부분 속성 설정

    private fun setButton() {
        binding.allClear.setOnClickListener(this)
        binding.cal.setOnClickListener(this)
        binding.cancel.setOnClickListener(this)
        binding.changeType.setOnClickListener(this)
        binding.decimalPoint.setOnClickListener(this)
        binding.extend.setOnClickListener(this)
        binding.inputBitAnd.setOnClickListener(this)
        binding.inputBitNot.setOnClickListener(this)
        binding.inputBitOr.setOnClickListener(this)
        binding.inputBitXor.setOnClickListener(this)
        binding.inputCloseParentheses.setOnClickListener(this)
        binding.inputCombination.setOnClickListener(this)
        binding.inputDivision.setOnClickListener(this)
        binding.inputEight.setOnClickListener(this)
        binding.inputFactorial.setOnClickListener(this)
        binding.inputFive.setOnClickListener(this)
        binding.inputFour.setOnClickListener(this)
        binding.inputGreatestCommonDiviser.setOnClickListener(this)
        binding.inputLeftShift.setOnClickListener(this)
        binding.inputLogarithm.setOnClickListener(this)
        binding.inputMinus.setOnClickListener(this)
        binding.inputModular.setOnClickListener(this)
        binding.inputMultiplication.setOnClickListener(this)
        binding.inputNine.setOnClickListener(this)
        binding.inputOne.setOnClickListener(this)
        binding.inputOpenParentheses.setOnClickListener(this)
        binding.inputPlus.setOnClickListener(this)
        binding.inputRightShift.setOnClickListener(this)
        binding.inputSeven.setOnClickListener(this)
        binding.inputSix.setOnClickListener(this)
        binding.inputSquared.setOnClickListener(this)
        binding.inputThree.setOnClickListener(this)
        binding.inputTwo.setOnClickListener(this)
        binding.inputZero.setOnClickListener(this)
        binding.variable.setOnClickListener(this)
    } //버튼 객체에 클릭 리스너 부여

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_calculator, container, false)
        calVar = Bundle(5)
        calVar.putDouble("x",0.0)
        calVar.putDouble("y",0.0)
        calVar.putDouble("z",0.0)
        calVar.putDouble("n",0.0)
        calVar.putDouble("m",0.0)
        //hide SoftKeyboard and long click disable
        setExpressionText()
        //setButtonClickListeners
        setButton()
        return binding.root
    }



}