package com.customerservice;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({UserTests.class, ClaimTests.class})
class CustomerServiceApplicationTests {
}
