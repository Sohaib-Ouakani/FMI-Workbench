package utility

/**
 * Synthesizes FMI (Functional Mock-up Interface) header files required for compilation.
 * Creates the necessary header files that define FMI types, functions, and function signatures.
 *
 * @param fs The [FilesystemManager] instance used to write header files to disk.
 */
class FmiHeaderSynthesiser(private val fs: FilesystemManager) {

    /**
     * Generates and writes FMI header files to the specified directory.
     * Creates fmi2FunctionTypes.h, fmi2Functions.h, and fmi2TypesPlatform.h files.
     *
     * @param dir The directory where the header files will be written.
     */
    fun synthesise(dir: String) {
        // Overwrite all headers
        fs.writeFile("$dir/fmi2FunctionTypes.h", FMI2_FUNCTION_TYPES)
        fs.writeFile("$dir/fmi2Functions.h", FMI2_FUNCTIONS)
        fs.writeFile("$dir/fmi2TypesPlatform.h", FMI2_TYPES_PLATFORM)
    }

    /**
     * Companion object containing the predefined FMI 2.0 header file contents.
     * These constants define the standard C header files required for FMI-compliant compilation.
     */
    companion object {
        private val FMI2_FUNCTION_TYPES = """
        #ifndef fmi2FunctionTypes_h
        #define fmi2FunctionTypes_h
        #include "fmi2TypesPlatform.h"
        #include <stdlib.h>
        typedef int fmi2Status;
        #define fmi2OK      0
        #define fmi2Warning 1
        #define fmi2Discard 2
        #define fmi2Error   3
        #define fmi2Fatal   4
        #define fmi2Pending 5
        typedef int fmi2Type;
        #define fmi2ModelExchange 0
        #define fmi2CoSimulation  1
        typedef int fmi2StatusKind;
        #define fmi2DoStepStatus       0
        #define fmi2PendingStatus      1
        #define fmi2LastSuccessfulTime 2
        #define fmi2Terminated         3
        typedef struct {
          fmi2Boolean newDiscreteStatesNeeded;
          fmi2Boolean terminateSimulation;
          fmi2Boolean nominalsOfContinuousStatesChanged;
          fmi2Boolean valuesOfContinuousStatesChanged;
          fmi2Boolean nextEventTimeDefined;
          fmi2Real    nextEventTime;
        } fmi2EventInfo;
        /* Function pointer typedefs */
        typedef const char* fmi2GetTypesPlatformTYPE(void);
        typedef const char* fmi2GetVersionTYPE(void);
        typedef fmi2Status  fmi2SetDebugLoggingTYPE(fmi2Component, fmi2Boolean, size_t, const fmi2String[]);
        typedef fmi2Component fmi2InstantiateTYPE(fmi2String, fmi2Type, fmi2String, fmi2String, const fmi2CallbackFunctions*, fmi2Boolean, fmi2Boolean);
        typedef void        fmi2FreeInstanceTYPE(fmi2Component);
        typedef fmi2Status  fmi2SetupExperimentTYPE(fmi2Component, fmi2Boolean, fmi2Real, fmi2Real, fmi2Boolean, fmi2Real);
        typedef fmi2Status  fmi2EnterInitializationModeTYPE(fmi2Component);
        typedef fmi2Status  fmi2ExitInitializationModeTYPE(fmi2Component);
        typedef fmi2Status  fmi2TerminateTYPE(fmi2Component);
        typedef fmi2Status  fmi2ResetTYPE(fmi2Component);
        typedef fmi2Status  fmi2GetRealTYPE(fmi2Component, const fmi2ValueReference[], size_t, fmi2Real[]);
        typedef fmi2Status  fmi2GetIntegerTYPE(fmi2Component, const fmi2ValueReference[], size_t, fmi2Integer[]);
        typedef fmi2Status  fmi2GetBooleanTYPE(fmi2Component, const fmi2ValueReference[], size_t, fmi2Boolean[]);
        typedef fmi2Status  fmi2GetStringTYPE(fmi2Component, const fmi2ValueReference[], size_t, fmi2String[]);
        typedef fmi2Status  fmi2SetRealTYPE(fmi2Component, const fmi2ValueReference[], size_t, const fmi2Real[]);
        typedef fmi2Status  fmi2SetIntegerTYPE(fmi2Component, const fmi2ValueReference[], size_t, const fmi2Integer[]);
        typedef fmi2Status  fmi2SetBooleanTYPE(fmi2Component, const fmi2ValueReference[], size_t, const fmi2Boolean[]);
        typedef fmi2Status  fmi2SetStringTYPE(fmi2Component, const fmi2ValueReference[], size_t, const fmi2String[]);
        typedef fmi2Status  fmi2GetFMUstateTYPE(fmi2Component, fmi2FMUstate*);
        typedef fmi2Status  fmi2SetFMUstateTYPE(fmi2Component, fmi2FMUstate);
        typedef fmi2Status  fmi2FreeFMUstateTYPE(fmi2Component, fmi2FMUstate*);
        typedef fmi2Status  fmi2SetRealInputDerivativesTYPE(fmi2Component, const fmi2ValueReference[], size_t, const fmi2Integer[], const fmi2Real[]);
        typedef fmi2Status  fmi2GetRealOutputDerivativesTYPE(fmi2Component, const fmi2ValueReference[], size_t, const fmi2Integer[], fmi2Real[]);
        typedef fmi2Status  fmi2DoStepTYPE(fmi2Component, fmi2Real, fmi2Real, fmi2Boolean);
        typedef fmi2Status  fmi2CancelStepTYPE(fmi2Component);
        typedef fmi2Status  fmi2GetStatusTYPE(fmi2Component, const fmi2StatusKind, fmi2Status*);
        typedef fmi2Status  fmi2GetRealStatusTYPE(fmi2Component, const fmi2StatusKind, fmi2Real*);
        typedef fmi2Status  fmi2GetIntegerStatusTYPE(fmi2Component, const fmi2StatusKind, fmi2Integer*);
        typedef fmi2Status  fmi2GetBooleanStatusTYPE(fmi2Component, const fmi2StatusKind, fmi2Boolean*);
        typedef fmi2Status  fmi2GetStringStatusTYPE(fmi2Component, const fmi2StatusKind, fmi2String*);

        // Model Exchange typedefs
        typedef fmi2Status fmi2SetTimeTYPE(fmi2Component, fmi2Real);
        typedef fmi2Status fmi2SetContinuousStatesTYPE(fmi2Component, const fmi2Real[], size_t);
        typedef fmi2Status fmi2EnterEventModeTYPE(fmi2Component);
        typedef fmi2Status fmi2NewDiscreteStatesTYPE(fmi2Component, fmi2EventInfo*);
        typedef fmi2Status fmi2EnterContinuousTimeModeTYPE(fmi2Component);
        typedef fmi2Status fmi2CompletedIntegratorStepTYPE(fmi2Component, fmi2Boolean, fmi2Boolean*, fmi2Boolean*);
        typedef fmi2Status fmi2GetDerivativesTYPE(fmi2Component, fmi2Real[], size_t);
        typedef fmi2Status fmi2GetEventIndicatorsTYPE(fmi2Component, fmi2Real[], size_t);
        typedef fmi2Status fmi2GetContinuousStatesTYPE(fmi2Component, fmi2Real[], size_t);
        typedef fmi2Status fmi2GetNominalsOfContinuousStatesTYPE(fmi2Component, fmi2Real[], size_t);
        // Serialization typedefs
        typedef fmi2Status fmi2SerializedFMUstateSizeTYPE(fmi2Component, fmi2FMUstate, size_t*);
        typedef fmi2Status fmi2SerializeFMUstateTYPE(fmi2Component, fmi2FMUstate, fmi2Byte[], size_t);
        typedef fmi2Status fmi2DeSerializeFMUstateTYPE(fmi2Component, const fmi2Byte[], size_t, fmi2FMUstate*);
        typedef fmi2Status fmi2GetDirectionalDerivativeTYPE(fmi2Component, const fmi2ValueReference[], size_t, const fmi2ValueReference[], size_t, const fmi2Real[], fmi2Real[]);
        #endif
        """.trimIndent()

        private val FMI2_FUNCTIONS = """
        #ifndef fmi2Functions_h
        #define fmi2Functions_h
        #include <stdlib.h>
        #include "fmi2TypesPlatform.h"
        #include "fmi2FunctionTypes.h"
        #define fmi2Version "2.0"
        #ifndef FMI2_Export
          #if defined _WIN32 || defined __CYGWIN__
            #define FMI2_Export __declspec(dllexport)
          #else
            #define FMI2_Export __attribute__((visibility("default")))
          #endif
        #endif
        
        /* Common functions */
        FMI2_Export const char* fmi2GetTypesPlatform(void);
        FMI2_Export const char* fmi2GetVersion(void);
        FMI2_Export fmi2Status  fmi2SetDebugLogging(fmi2Component, fmi2Boolean, size_t, const fmi2String[]);
        FMI2_Export fmi2Component fmi2Instantiate(fmi2String, fmi2Type, fmi2String, fmi2String, const fmi2CallbackFunctions*, fmi2Boolean, fmi2Boolean);
        FMI2_Export void        fmi2FreeInstance(fmi2Component);
        FMI2_Export fmi2Status  fmi2SetupExperiment(fmi2Component, fmi2Boolean, fmi2Real, fmi2Real, fmi2Boolean, fmi2Real);
        FMI2_Export fmi2Status  fmi2EnterInitializationMode(fmi2Component);
        FMI2_Export fmi2Status  fmi2ExitInitializationMode(fmi2Component);
        FMI2_Export fmi2Status  fmi2Terminate(fmi2Component);
        FMI2_Export fmi2Status  fmi2Reset(fmi2Component);
        
        /* Getters/Setters */
        FMI2_Export fmi2Status fmi2GetReal   (fmi2Component, const fmi2ValueReference[], size_t, fmi2Real[]);
        FMI2_Export fmi2Status fmi2GetInteger(fmi2Component, const fmi2ValueReference[], size_t, fmi2Integer[]);
        FMI2_Export fmi2Status fmi2GetBoolean(fmi2Component, const fmi2ValueReference[], size_t, fmi2Boolean[]);
        FMI2_Export fmi2Status fmi2GetString (fmi2Component, const fmi2ValueReference[], size_t, fmi2String[]);
        FMI2_Export fmi2Status fmi2SetReal   (fmi2Component, const fmi2ValueReference[], size_t, const fmi2Real[]);
        FMI2_Export fmi2Status fmi2SetInteger(fmi2Component, const fmi2ValueReference[], size_t, const fmi2Integer[]);
        FMI2_Export fmi2Status fmi2SetBoolean(fmi2Component, const fmi2ValueReference[], size_t, const fmi2Boolean[]);
        FMI2_Export fmi2Status fmi2SetString (fmi2Component, const fmi2ValueReference[], size_t, const fmi2String[]);
        
        /* Co-Simulation functions */
        FMI2_Export fmi2Status fmi2SetRealInputDerivatives (fmi2Component, const fmi2ValueReference[], size_t, const fmi2Integer[], const fmi2Real[]);
        FMI2_Export fmi2Status fmi2GetRealOutputDerivatives(fmi2Component, const fmi2ValueReference[], size_t, const fmi2Integer[], fmi2Real[]);
        FMI2_Export fmi2Status fmi2DoStep    (fmi2Component, fmi2Real, fmi2Real, fmi2Boolean);
        FMI2_Export fmi2Status fmi2CancelStep(fmi2Component);
        FMI2_Export fmi2Status fmi2GetStatus       (fmi2Component, const fmi2StatusKind, fmi2Status*);
        FMI2_Export fmi2Status fmi2GetRealStatus   (fmi2Component, const fmi2StatusKind, fmi2Real*);
        FMI2_Export fmi2Status fmi2GetIntegerStatus(fmi2Component, const fmi2StatusKind, fmi2Integer*);
        FMI2_Export fmi2Status fmi2GetBooleanStatus(fmi2Component, const fmi2StatusKind, fmi2Boolean*);
        FMI2_Export fmi2Status fmi2GetStringStatus (fmi2Component, const fmi2StatusKind, fmi2String*);
        #endif
        """.trimIndent()

        private val FMI2_TYPES_PLATFORM = """
        #ifndef fmi2TypesPlatform_h
        #define fmi2TypesPlatform_h
        #include <stddef.h>
        #define fmi2TypesPlatform "default"
        typedef double           fmi2Real;
        typedef int              fmi2Integer;
        typedef int              fmi2Boolean;
        typedef char             fmi2Char;
        typedef const fmi2Char*  fmi2String;
        typedef char             fmi2Byte;
        #define fmi2True  1
        #define fmi2False 0
        typedef void* fmi2Component;
        typedef void* fmi2ComponentEnvironment;
        typedef void* fmi2FMUstate;
        typedef unsigned int fmi2ValueReference;
        typedef void  (*fmi2CallbackLogger)(fmi2ComponentEnvironment,fmi2String,int,fmi2String,fmi2String,...);
        typedef void* (*fmi2CallbackAllocateMemory)(size_t,size_t);
        typedef void  (*fmi2CallbackFreeMemory)(void*);
        typedef void  (*fmi2StepFinished)(fmi2ComponentEnvironment,int);
        typedef struct {
          fmi2CallbackLogger         logger;
          fmi2CallbackAllocateMemory allocateMemory;
          fmi2CallbackFreeMemory     freeMemory;
          fmi2StepFinished           stepFinished;
          fmi2ComponentEnvironment   componentEnvironment;
        } fmi2CallbackFunctions;
        #endif
        """.trimIndent()
    }
}
