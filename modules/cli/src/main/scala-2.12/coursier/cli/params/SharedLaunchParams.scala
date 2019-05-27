package coursier.cli.params

import java.nio.file.{Path, Paths}

import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import coursier.cli.fetch.FetchParams
import coursier.cli.options.SharedLaunchOptions
import coursier.cli.resolve.ResolveParams

final case class SharedLaunchParams(
  resolve: ResolveParams,
  artifact: ArtifactParams,
  sharedLoader: SharedLoaderParams,
  mainClassOpt: Option[String],
  properties: Seq[(String, String)],
  extraJars: Seq[Path]
) {
  def fetch: FetchParams =
    FetchParams(
      classpath = false,
      jsonOutputOpt = None,
      resolve = resolve,
      artifact = artifact
    )
}

object SharedLaunchParams {
  def apply(options: SharedLaunchOptions): ValidatedNel[String, SharedLaunchParams] = {

    val resolveV = ResolveParams(options.resolveOptions)
    val artifactV = ArtifactParams(options.artifactOptions)
    val sharedLoaderV = resolveV.map(r => (r.dependency, r.resolution)).toOption match {
      case None =>
        Validated.validNel(SharedLoaderParams(Nil, Map.empty))
      case Some((depsOpts, resolutionOpts)) =>
        SharedLoaderParams(options.sharedLoaderOptions, resolutionOpts.selectedScalaVersion, depsOpts.defaultConfiguration)
    }

    val mainClassOpt = Some(options.mainClass).filter(_.nonEmpty)

    val propertiesV = options.property.traverse { s =>
      val idx = s.indexOf('=')
      if (idx < 0)
        Validated.invalidNel(s"Malformed property argument '$s' (expected name=value)")
      else
        Validated.validNel(s.substring(0, idx) -> s.substring(idx + 1))
    }

    // check if those exist?
    val extraJars = options.extraJars.map { p =>
      Paths.get(p)
    }

    (resolveV, artifactV, sharedLoaderV, propertiesV).mapN {
      (resolve, artifact, sharedLoader, properties) =>
        SharedLaunchParams(
          resolve,
          artifact,
          sharedLoader,
          mainClassOpt,
          properties,
          extraJars
        )
    }
  }
}
