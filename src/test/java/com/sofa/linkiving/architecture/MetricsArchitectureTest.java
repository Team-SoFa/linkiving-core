package com.sofa.linkiving.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import io.micrometer.core.instrument.Counter;

@AnalyzeClasses(
	packages = "com.sofa.linkiving",
	importOptions = ImportOption.DoNotIncludeTests.class
)
class MetricsArchitectureTest {

	@ArchTest
	static final ArchRule counterBuilderIsOnlyCalledFromMetricsFactories =
		noClasses()
			.that().resideOutsideOfPackage("..global.metrics..")
			.should().callMethod(Counter.class, "builder", String.class)
			.because("메트릭 등록은 전용 팩토리(AiClientMetrics, AsyncTaskMetrics)를 경유해야 "
				+ "태그 키셋이 컴파일 타임에 강제된다. Counter.builder 직접 호출은 "
				+ "동일 메트릭 이름에 다른 태그 키셋이 등록되는 문제를 재발시킨다 (#261, #263)");
}
