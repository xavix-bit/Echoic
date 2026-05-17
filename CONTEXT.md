# Echoic TTS

Echoic is a desktop text-to-speech app that can synthesize speech through cloud providers or local model runtimes.

## Language

**TTS Provider**:
A cloud speech provider that accepts text and returns synthesized audio.
_Avoid_: Cloud service, vendor

**Local TTS Provider**:
An on-device speech runtime family with installable model assets and platform constraints.
_Avoid_: Local engine, offline service

**TTS Model**:
A provider-specific synthesis model identifier sent to a TTS Provider.
_Avoid_: Engine, preset

**Voice**:
A provider-specific speaker identity used during synthesis.
_Avoid_: Speaker, persona

**Local Model**:
Downloaded filesystem assets required by a Local TTS Provider.
_Avoid_: Cache, bundle

**Download Source**:
A mirror or repository location used to obtain Local Model files.
_Avoid_: URL, mirror when the source also carries priority/description

**Synthesis Session**:
A single speech generation operation from selection and text to audio data.
_Avoid_: Request, job

**Provider Catalog**:
The authoritative list of available cloud and local synthesis options plus their tags and defaults.
_Avoid_: Enum list, model list

## Relationships

- A **TTS Provider** has zero or more **TTS Models** and **Voices**
- A **Local TTS Provider** requires one **Local Model** installation before synthesis
- A **Download Source** provides files for one **Local Model**
- A **Provider Catalog** exposes **TTS Providers** and **Local TTS Providers** as synthesis options
- A **Synthesis Session** uses either one **TTS Provider** with a **TTS Model** and **Voice**, or one **Local TTS Provider** with an installed **Local Model**

## Example Dialogue

> **Dev:** "When user picks Sherpa-ONNX, do we ask Provider Catalog for a TTS Model?"
> **Domain expert:** "No. Sherpa-ONNX is a Local TTS Provider; the Synthesis Session needs its installed Local Model, not a cloud TTS Model."

## Flagged Ambiguities

- "model" can mean **TTS Model** or **Local Model**. Use **TTS Model** for provider request IDs and **Local Model** for installed files.
- "provider" can mean **TTS Provider** or **Local TTS Provider**. Use the qualified term unless cloud/local is already explicit.
