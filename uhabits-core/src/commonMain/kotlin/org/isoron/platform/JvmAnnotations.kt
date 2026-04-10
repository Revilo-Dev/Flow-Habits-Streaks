package org.isoron.platform

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.SOURCE)
expect annotation class Synchronized()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.SOURCE)
expect annotation class JvmStatic()
