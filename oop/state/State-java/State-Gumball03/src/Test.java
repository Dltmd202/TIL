/**
 * 한국기술교육대학교 컴퓨터공학부 객체지향개발론및실습
 * 2022년도 2학기
 * @author 김상진
 * 상태 패턴
 * State Driven Transition (상태 기반 전이)
 * Test.java
 * 테스트 프로그램
 */
public class Test {
	public static void main(String[] args) {
		GumballMachine myMachine = new GumballMachine(5);
		myMachine.insertCoin();
		myMachine.turnCrank();
		myMachine.insertCoin();
		myMachine.turnCrank();
		myMachine.insertCoin();
		myMachine.turnCrank();
		myMachine.insertCoin();
		myMachine.turnCrank();
		myMachine.insertCoin();
		myMachine.turnCrank();
		myMachine.insertCoin();
		myMachine.turnCrank();
		myMachine.refill();
	}

}
