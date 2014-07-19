require 'java'

class RbExample
  include Java::Zombie::Player
  def doTurn(context)
    Java::Zombie::Move::STAY
  end
end

Java::Zombie::PlayerRegistry.registerPlayer('rb-example', RbExample.new)