/**
 * 한국기술교육대학교 컴퓨터공학부 객체지향개발론및실습
 * 2022년도 2학기
 * 상태 패턴
 * GumballState.java
 * 컴볼기기들의 상태가 제공해야 하는 interface
 * Context Driven Transition (문맥 중심 전이)
 * @author 김상진
 */
public interface GumballState {
	boolean insertCoin(GumballMachine gumballMachine);
	boolean ejectCoin(GumballMachine gumballMachine);
	boolean turnCrank(GumballMachine gumballMachine);
	boolean dispense(GumballMachine gumballMachine);
	boolean refill(GumballMachine gumballMachine);
}