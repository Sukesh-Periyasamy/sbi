"""Add campaign relationship

Revision ID: 68f136c1d4c8
Revises: bfca113920df
Create Date: 2026-07-04 04:56:06.942850

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '68f136c1d4c8'
down_revision: Union[str, Sequence[str], None] = 'bfca113920df'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    with op.batch_alter_table('intelligence', schema=None) as batch_op:
        batch_op.add_column(sa.Column('campaign_id', sa.Integer(), nullable=True))
        batch_op.create_foreign_key('fk_intel_campaign', 'campaigns', ['campaign_id'], ['id'])


def downgrade() -> None:
    """Downgrade schema."""
    with op.batch_alter_table('intelligence', schema=None) as batch_op:
        batch_op.drop_constraint('fk_intel_campaign', type_='foreignkey')
        batch_op.drop_column('campaign_id')
