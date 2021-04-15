package satorg.cateffay.concurrent

import cats.effect.kernel._
import cats.effect.std.Hotswap
import cats.syntax.all._

import scala.concurrent.duration.FiniteDuration

final class RefreRef[F[_], A] private (
    fetch: F[(A, FiniteDuration)],
    refresher: Hotswap[F, Unit],
    ref: Ref[F, Either[Throwable, A]]
)(implicit F: Async[F])
    extends RefSource[F, A] {

  def get: F[A] = ref.get.rethrow

  def refresh: F[Unit] = {
    fetch
      .redeemWith(
        { err =>
          ref.set(Left(err)) *> refresher.clear
        },
        { case (a, pause) =>
          ref.set(Right(a)) *> refresher.swap(scheduleRefresh(pause))
        }
      )
  }

  private def scheduleRefresh(pause: FiniteDuration): Resource[F, Unit] =
    F.background(F.delayBy(refresh, pause)) *> Resource.unit
}

object RefreRef {

  def resource[F[_], A](fetch: F[(A, FiniteDuration)])(implicit F: Async[F]): Resource[F, RefreRef[F, A]] = {
    Hotswap
      .create[F, Unit]
      .evalMap { refresher =>
        fetch.flatMap { case (a, pause) =>
          F.ref(a.asRight[Throwable]).map(new RefreRef(fetch, refresher, _) -> pause)
        }
      }
      .flatMap { case (result, pause) =>
        result.scheduleRefresh(pause).as(result)
      }
  }

  def periodicResource[F[_]: Async, A](fetch: F[A], period: FiniteDuration): Resource[F, RefreRef[F, A]] =
    resource(fetch.map(_ -> period))
}
